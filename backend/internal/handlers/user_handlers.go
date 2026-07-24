package handlers

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/models"
)

// ---------------------------------------------------------------------------
// Rotas de usuário: ver o próprio perfil, buscar outro usuário (e pegar a
// chave pública dele, necessária pra criptografar mensagens pra ele).
// ---------------------------------------------------------------------------

// Me devolve os dados do usuário logado.
func (h *Handlers) Me(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	u, err := h.getUserByID(userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar perfil")
		return
	}
	writeJSON(w, http.StatusOK, u)
}

// GetUser busca um usuário pelo ID ou username. Se o que vem depois de
// /api/users/ for numérico, trata como ID. Senão, trata como username.
// Retorna a chave pública (necessária pra criptografar mensagens).
func (h *Handlers) GetUser(w http.ResponseWriter, r *http.Request) {
	// A rota é /api/users/{idOrUsername}. Pegamos a última parte do caminho.
	param := strings.TrimPrefix(r.URL.Path, "/api/users/")
	param = strings.TrimSpace(param)
	if param == "" {
		writeError(w, http.StatusBadRequest, "informe o ID ou nome de usuário")
		return
	}

	var u models.User

	// Tenta parsear como ID primeiro.
	if userID, err := strconv.ParseInt(param, 10, 64); err == nil && userID > 0 {
		err := h.DB.QueryRow(
			`SELECT id, username, name, public_key, avatar_url, created_at
			 FROM users WHERE id = ?`,
			userID,
		).Scan(&u.ID, &u.Username, &u.Name, &u.PublicKey, &u.AvatarURL, &u.CreatedAt)

		if errors.Is(err, sql.ErrNoRows) {
			writeError(w, http.StatusNotFound, "usuário não encontrado")
			return
		}
		if err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao buscar usuário")
			return
		}
		writeJSON(w, http.StatusOK, u)
		return
	}

	// Senão, trata como username.
	err := h.DB.QueryRow(
		`SELECT id, username, name, public_key, avatar_url, created_at
		 FROM users WHERE username = ?`,
		param,
	).Scan(&u.ID, &u.Username, &u.Name, &u.PublicKey, &u.AvatarURL, &u.CreatedAt)

	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusNotFound, "usuário não encontrado")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao buscar usuário")
		return
	}

	writeJSON(w, http.StatusOK, u)
}

// getUserByID é um helper interno pra carregar um usuário completo pelo ID.
func (h *Handlers) getUserByID(id int64) (*models.User, error) {
	var u models.User
	err := h.DB.QueryRow(
		`SELECT id, username, name, birthday, email, phone, public_key, avatar_url, created_at
		 FROM users WHERE id = ?`,
		id,
	).Scan(&u.ID, &u.Username, &u.Name, &u.Birthday, &u.Email, &u.Phone, &u.PublicKey, &u.AvatarURL, &u.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &u, nil
}
