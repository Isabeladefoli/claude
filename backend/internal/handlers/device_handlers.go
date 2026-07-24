package handlers

import (
	"log"
	"net/http"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// deviceTokenRequest: cliente manda seu token FCM pro servidor guardar.
type deviceTokenRequest struct {
	Token string `json:"token"` // token do Firebase Cloud Messaging
}

// RegisterDeviceToken recebe o token FCM do app e o guarda pra notificações.
// Chamado quando o app abre e gera um novo token (ou ao fazer login).
func (h *Handlers) RegisterDeviceToken(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req deviceTokenRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.Token == "" {
		writeError(w, http.StatusBadRequest, "faltando token")
		return
	}

	// Se o token já existe (mesmo aparelho, mesmo usuário), só retorna ok.
	// Se é novo, insere.
	_, err := h.DB.Exec(
		`INSERT OR IGNORE INTO device_tokens (user_id, token) VALUES (?, ?)`,
		userID, req.Token,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao registrar device")
		return
	}

	log.Printf("[device] token FCM registrado para usuário %d", userID)
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}
