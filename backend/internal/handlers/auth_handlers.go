package handlers

import (
	"database/sql"
	"errors"
	"net/http"
	"regexp"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// ---------------------------------------------------------------------------
// Rotas de conta: registrar e logar.
//
// Regras que você pediu:
//   - Login com NOME DE USUÁRIO e SENHA (não precisa email).
//   - Email e telefone são OPCIONAIS.
//   - Cada usuário envia sua CHAVE PÚBLICA no registro (gerada no celular).
// ---------------------------------------------------------------------------

// usernameRegex: letras, números, ponto, hífen e underline; de 3 a 20 chars.
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9._-]{3,20}$`)

// registerRequest é o formato do JSON que o app manda pra criar conta.
type registerRequest struct {
	Username  string  `json:"username"`
	Password  string  `json:"password"`
	PublicKey string  `json:"public_key"`     // chave pública gerada no celular
	Email     *string `json:"email,omitempty"`
	Phone     *string `json:"phone,omitempty"`
}

// authResponse é o que devolvemos após registrar/logar: o token e o ID.
type authResponse struct {
	Token  string `json:"token"`
	UserID int64  `json:"user_id"`
}

// Register cria uma conta nova.
func (h *Handlers) Register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if !decodeJSON(w, r, &req) {
		return
	}

	// --- Validações de entrada (nunca confie no que o cliente manda) ---
	req.Username = strings.TrimSpace(req.Username)
	if !usernameRegex.MatchString(req.Username) {
		writeError(w, http.StatusBadRequest,
			"nome de usuário deve ter 3-20 caracteres (letras, números, . _ -)")
		return
	}
	if len(req.Password) < 8 {
		writeError(w, http.StatusBadRequest, "a senha precisa ter ao menos 8 caracteres")
		return
	}
	if req.PublicKey == "" {
		writeError(w, http.StatusBadRequest, "faltando a chave pública (public_key)")
		return
	}

	// Transforma a senha em hash Argon2id antes de tocar no banco.
	hash, err := auth.HashPassword(req.Password)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao processar senha")
		return
	}

	// Insere o usuário. Se o username já existir, o UNIQUE do banco barra.
	res, err := h.DB.Exec(
		`INSERT INTO users (username, password_hash, email, phone, public_key)
		 VALUES (?, ?, ?, ?, ?)`,
		req.Username, hash, req.Email, req.Phone, req.PublicKey,
	)
	if err != nil {
		// Erro mais comum aqui: username duplicado.
		if strings.Contains(err.Error(), "UNIQUE") {
			writeError(w, http.StatusConflict, "esse nome de usuário já está em uso")
			return
		}
		writeError(w, http.StatusInternalServerError, "erro ao criar conta")
		return
	}

	userID, _ := res.LastInsertId()

	// Já entrega um token pra pessoa entrar direto após registrar.
	token, err := h.Tokens.Generate(userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao gerar sessão")
		return
	}

	writeJSON(w, http.StatusCreated, authResponse{Token: token, UserID: userID})
}

type loginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// Login verifica usuário + senha e devolve um token novo.
func (h *Handlers) Login(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	req.Username = strings.TrimSpace(req.Username)

	// Busca o usuário pelo nome.
	var (
		userID int64
		hash   string
	)
	err := h.DB.QueryRow(
		`SELECT id, password_hash FROM users WHERE username = ?`,
		req.Username,
	).Scan(&userID, &hash)

	// IMPORTANTE: damos a MESMA mensagem de erro se o usuário não existe OU se
	// a senha está errada. Assim um atacante não descobre quais nomes existem
	// (isso se chama evitar "user enumeration").
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusUnauthorized, "usuário ou senha inválidos")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao consultar conta")
		return
	}

	ok, err := auth.VerifyPassword(req.Password, hash)
	if err != nil || !ok {
		writeError(w, http.StatusUnauthorized, "usuário ou senha inválidos")
		return
	}

	token, err := h.Tokens.Generate(userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao gerar sessão")
		return
	}

	writeJSON(w, http.StatusOK, authResponse{Token: token, UserID: userID})
}
