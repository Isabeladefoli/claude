package handlers

import (
	"crypto/rand"
	"database/sql"
	"encoding/hex"
	"errors"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// ---------------------------------------------------------------------------
// Mídia: subir (upload) e baixar (download) arquivos — fotos, áudios, vídeos e
// foto de perfil. Regra de ouro: o ARQUIVO mora no disco (pasta de mídia); o
// banco guarda só um "vale" com os dados dele. Assim o banco não incha.
// ---------------------------------------------------------------------------

// Limite por upload (25 MB). Evita que alguém encha o disco do seu PC de uma vez.
const maxUploadBytes = 25 << 20 // 25 * 1024 * 1024

// O id só pode ser hex (é o que a gente gera). Isso também barra path traversal
// no download (ninguém consegue pedir "../../algo" e sair da pasta de mídia).
var mediaIDRegex = regexp.MustCompile(`^[a-f0-9]{32}$`)

// UploadMedia recebe um arquivo (multipart, campo "file"), grava no disco e
// devolve { "id": "...", "url": "/api/media/..." }.
func (h *Handlers) UploadMedia(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	// Limita o corpo inteiro (proteção contra upload gigante).
	r.Body = http.MaxBytesReader(w, r.Body, maxUploadBytes+1024)

	file, header, err := r.FormFile("file")
	if err != nil {
		writeError(w, http.StatusBadRequest, "faltando o arquivo (campo 'file')")
		return
	}
	defer file.Close()

	// Tipo do arquivo: usa o que o cliente informou; se vazio, um genérico.
	mime := header.Header.Get("Content-Type")
	if mime == "" {
		mime = "application/octet-stream"
	}

	// Gera um id aleatório (16 bytes -> 32 hex). Também é o nome no disco.
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao gerar id")
		return
	}
	id := hex.EncodeToString(buf)

	// Garante a pasta e grava o arquivo.
	dir := h.mediaDir()
	if err := os.MkdirAll(dir, 0o755); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao preparar pasta")
		return
	}
	fullPath := filepath.Join(dir, id)
	dst, err := os.Create(fullPath)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao salvar arquivo")
		return
	}
	size, err := io.Copy(dst, file)
	dst.Close()
	if err != nil {
		os.Remove(fullPath) // não deixa lixo pela metade no disco
		writeError(w, http.StatusRequestEntityTooLarge, "arquivo muito grande ou erro ao salvar")
		return
	}

	// Registra no banco.
	if _, err := h.DB.Exec(
		`INSERT INTO media (id, owner_id, mime, size) VALUES (?, ?, ?, ?)`,
		id, userID, mime, size,
	); err != nil {
		os.Remove(fullPath)
		writeError(w, http.StatusInternalServerError, "erro ao registrar mídia")
		return
	}

	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"id":   id,
		"url":  "/api/media/" + id,
		"mime": mime,
		"size": size,
	})
}

// ServeMedia devolve o arquivo pelo id. É PÚBLICO de propósito: o id é aleatório
// e não-adivinhável (funciona como uma "URL secreta"). Quando o E2E entrar, o
// conteúdo estará cifrado — nem o servidor nem quem pegar a URL vê nada.
func (h *Handlers) ServeMedia(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if !mediaIDRegex.MatchString(id) {
		writeError(w, http.StatusNotFound, "mídia não encontrada")
		return
	}

	var mime string
	err := h.DB.QueryRow(`SELECT mime FROM media WHERE id = ?`, id).Scan(&mime)
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusNotFound, "mídia não encontrada")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao buscar mídia")
		return
	}

	f, err := os.Open(filepath.Join(h.mediaDir(), id))
	if err != nil {
		writeError(w, http.StatusNotFound, "arquivo não encontrado")
		return
	}
	defer f.Close()

	w.Header().Set("Content-Type", mime)
	w.Header().Set("Cache-Control", "private, max-age=31536000, immutable")
	io.Copy(w, f)
}
