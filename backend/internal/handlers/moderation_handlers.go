package handlers

import (
	"net/http"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// ---------------------------------------------------------------------------
// Moderação: bloquear/desbloquear e denunciar usuários.
//
// Bloqueio é mútuo no efeito: se A bloqueia B, nenhum dos dois consegue mandar
// mensagem pro outro (o SendMessage confere isso).
// ---------------------------------------------------------------------------

// BlockUser: o usuário logado bloqueia outro.
func (h *Handlers) BlockUser(w http.ResponseWriter, r *http.Request) {
	me, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	if otherID == me {
		writeError(w, http.StatusBadRequest, "não dá pra bloquear você mesmo")
		return
	}
	if _, err := h.DB.Exec(
		`INSERT OR IGNORE INTO blocks (blocker_id, blocked_id) VALUES (?, ?)`, me, otherID,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao bloquear")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// UnblockUser: desfaz o bloqueio.
func (h *Handlers) UnblockUser(w http.ResponseWriter, r *http.Request) {
	me, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	if _, err := h.DB.Exec(
		`DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?`, me, otherID,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao desbloquear")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// ListBlocks devolve os ids que o usuário logado bloqueou (o app usa pra mostrar
// "Bloquear" ou "Desbloquear" no perfil).
func (h *Handlers) ListBlocks(w http.ResponseWriter, r *http.Request) {
	me, _ := auth.UserIDFromContext(r.Context())
	rows, err := h.DB.Query(`SELECT blocked_id FROM blocks WHERE blocker_id = ?`, me)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao listar bloqueios")
		return
	}
	defer rows.Close()
	ids := make([]int64, 0)
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err == nil {
			ids = append(ids, id)
		}
	}
	writeJSON(w, http.StatusOK, map[string][]int64{"blocked": ids})
}

// ReportUser guarda uma denúncia contra outro usuário.
func (h *Handlers) ReportUser(w http.ResponseWriter, r *http.Request) {
	me, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "id")
	if !ok {
		return
	}
	var req struct {
		Reason string `json:"reason"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	reason := strings.TrimSpace(req.Reason)
	if reason == "" {
		writeError(w, http.StatusBadRequest, "escreva o motivo da denúncia")
		return
	}
	if _, err := h.DB.Exec(
		`INSERT INTO reports (reporter_id, reported_id, reason) VALUES (?, ?, ?)`,
		me, otherID, reason,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao enviar denúncia")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]bool{"ok": true})
}

// isBlockedBetween diz se há bloqueio em qualquer direção entre a e b.
func (h *Handlers) isBlockedBetween(a, b int64) bool {
	var x int
	h.DB.QueryRow(
		`SELECT 1 FROM blocks WHERE (blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?)`,
		a, b, b, a,
	).Scan(&x)
	return x == 1
}
