package handlers

import (
	"database/sql"
	"errors"
	"net/http"
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

// GetUserByUsername permite achar outra pessoa pelo nome de usuário. Retorna
// o perfil público dela — incluindo a CHAVE PÚBLICA, que o app usa pra
// criptografar mensagens destinadas a essa pessoa.
func (h *Handlers) GetUserByUsername(w http.ResponseWriter, r *http.Request) {
	// A rota é /api/users/{username}. Pegamos a última parte do caminho.
	username := strings.TrimPrefix(r.URL.Path, "/api/users/")
	username = strings.TrimSpace(username)
	if username == "" {
		writeError(w, http.StatusBadRequest, "informe o nome de usuário")
		return
	}

	var u models.User
	err := h.DB.QueryRow(
		`SELECT id, username, name, public_key, avatar_url, created_at
		 FROM users WHERE username = ?`,
		username,
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
