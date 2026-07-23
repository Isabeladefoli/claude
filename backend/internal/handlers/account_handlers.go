package handlers

import (
	"net/http"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// ---------------------------------------------------------------------------
// Rotas de conta (tela "Account details"): atualizar perfil, trocar senha,
// conferir a senha (trava pra entrar na tela) e apagar a conta.
//
// Detalhe de segurança que aparece o tempo todo aqui: qualquer ação sensível
// (ver dados sensíveis, trocar senha, apagar conta) EXIGE a senha atual de novo.
// Ter o token não basta — o celular pode estar destravado na mão de outra
// pessoa. Pedir a senha de novo é a "confirmação de que é você mesmo".
// ---------------------------------------------------------------------------

// verifyRequest carrega só a senha, usada nas ações que pedem confirmação.
type verifyRequest struct {
	Password string `json:"password"`
}

// checkPassword confere se a senha bate com a do usuário logado. Devolve true
// se estiver certa. Em erro de banco/hash devolve false (falha fechada).
func (h *Handlers) checkPassword(userID int64, password string) bool {
	var hash string
	if err := h.DB.QueryRow(`SELECT password_hash FROM users WHERE id = ?`, userID).
		Scan(&hash); err != nil {
		return false
	}
	ok, err := auth.VerifyPassword(password, hash)
	return err == nil && ok
}

// VerifyPassword é a "trava" da tela Account details: o app manda a senha e a
// gente responde 200 (pode entrar) ou 401 (senha errada). Não devolve dados.
func (h *Handlers) VerifyPassword(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req verifyRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if !h.checkPassword(userID, req.Password) {
		writeError(w, http.StatusUnauthorized, "senha incorreta")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// updateProfileRequest: só mexemos nos campos que vierem preenchidos (ponteiro
// não-nulo). Assim o app pode mandar só o que mudou.
type updateProfileRequest struct {
	Username  *string `json:"username,omitempty"`
	Name      *string `json:"name,omitempty"`
	Birthday  *string `json:"birthday,omitempty"`
	AvatarURL *string `json:"avatar_url,omitempty"`
}

// UpdateProfile atualiza nome de usuário, nome de exibição, aniversário e/ou
// foto. Devolve o perfil atualizado.
func (h *Handlers) UpdateProfile(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req updateProfileRequest
	if !decodeJSON(w, r, &req) {
		return
	}

	// Monta o UPDATE só com os campos enviados (SET dinâmico).
	sets := make([]string, 0, 4)
	args := make([]interface{}, 0, 5)

	if req.Username != nil {
		newName := strings.TrimSpace(*req.Username)
		if !usernameRegex.MatchString(newName) {
			writeError(w, http.StatusBadRequest,
				"nome de usuário deve ter 3-30 caracteres (letras, números, . _ -)")
			return
		}
		sets = append(sets, "username = ?")
		args = append(args, newName)
	}
	if req.Name != nil {
		sets = append(sets, "name = ?")
		args = append(args, trimmedOrNil(req.Name))
	}
	if req.Birthday != nil {
		sets = append(sets, "birthday = ?")
		args = append(args, trimmedOrNil(req.Birthday))
	}
	if req.AvatarURL != nil {
		sets = append(sets, "avatar_url = ?")
		args = append(args, trimmedOrNil(req.AvatarURL))
	}

	if len(sets) == 0 {
		writeError(w, http.StatusBadRequest, "nada para atualizar")
		return
	}

	args = append(args, userID)
	_, err := h.DB.Exec(`UPDATE users SET `+strings.Join(sets, ", ")+` WHERE id = ?`, args...)
	if err != nil {
		if strings.Contains(err.Error(), "UNIQUE") {
			writeError(w, http.StatusConflict, "esse nome de usuário já está em uso")
			return
		}
		writeError(w, http.StatusInternalServerError, "erro ao atualizar perfil")
		return
	}

	u, err := h.getUserByID(userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar perfil")
		return
	}
	writeJSON(w, http.StatusOK, u)
}

// changePasswordRequest exige a senha atual (confirmação) e a nova.
type changePasswordRequest struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}

// ChangePassword troca a senha depois de conferir a atual.
func (h *Handlers) ChangePassword(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req changePasswordRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if !h.checkPassword(userID, req.CurrentPassword) {
		writeError(w, http.StatusUnauthorized, "senha atual incorreta")
		return
	}
	if len(req.NewPassword) < 8 {
		writeError(w, http.StatusBadRequest, "a nova senha precisa ter ao menos 8 caracteres")
		return
	}

	hash, err := auth.HashPassword(req.NewPassword)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao processar senha")
		return
	}
	if _, err := h.DB.Exec(`UPDATE users SET password_hash = ? WHERE id = ?`, hash, userID); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao salvar senha")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// DeleteAccount apaga a conta (e, por cascata do banco, mensagens, contatos e
// participações em grupo). Exige a senha como confirmação — é irreversível.
func (h *Handlers) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req verifyRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if !h.checkPassword(userID, req.Password) {
		writeError(w, http.StatusUnauthorized, "senha incorreta")
		return
	}
	if _, err := h.DB.Exec(`DELETE FROM users WHERE id = ?`, userID); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao apagar conta")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}
