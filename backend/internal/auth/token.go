package auth

import (
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// ---------------------------------------------------------------------------
// A LÓGICA das sessões de login (JWT):
//
// Depois que o usuário faz login, não queremos pedir a senha a cada mensagem.
// Então entregamos um "token": um crachá assinado pelo servidor que diz
// "quem apresentar isto é o usuário 42".
//
// Usamos JWT (JSON Web Token). Ele tem 3 partes separadas por ponto:
//   cabeçalho.dados.assinatura
// A "assinatura" é feita com o nosso segredo (JWTSecret). Como só o servidor
// tem o segredo, ninguém consegue forjar ou alterar um token. Se mudar 1 letra
// dos dados, a assinatura não bate mais e rejeitamos.
//
// O token tem validade (expira). Assim, se vazar, o estrago é limitado no tempo.
// ---------------------------------------------------------------------------

const tokenValidity = 30 * 24 * time.Hour // 30 dias

// Claims são os dados que colocamos dentro do token. Guardamos só o ID do
// usuário — nada sensível, porque o conteúdo do JWT é legível por qualquer um
// (ele é só assinado, não criptografado).
type Claims struct {
	UserID int64 `json:"uid"`
	jwt.RegisteredClaims
}

// TokenManager cria e valida tokens usando o segredo do servidor.
type TokenManager struct {
	secret []byte
}

func NewTokenManager(secret []byte) *TokenManager {
	return &TokenManager{secret: secret}
}

// Generate cria um token novo para um usuário.
func (t *TokenManager) Generate(userID int64) (string, error) {
	claims := Claims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(tokenValidity)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(t.secret)
}

var ErrInvalidToken = errors.New("token inválido ou expirado")

// Parse valida o token e devolve o ID do usuário dentro dele.
func (t *TokenManager) Parse(tokenStr string) (int64, error) {
	claims := &Claims{}
	token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
		// Confirmamos que o algoritmo de assinatura é o que esperamos (HS256).
		// Isso bloqueia um ataque clássico onde o atacante troca o algoritmo
		// para "none" e manda um token sem assinatura.
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, ErrInvalidToken
		}
		return t.secret, nil
	})
	if err != nil || !token.Valid {
		return 0, ErrInvalidToken
	}
	return claims.UserID, nil
}

// tokenFromRequest extrai o token do cabeçalho "Authorization: Bearer <token>".
func tokenFromRequest(r *http.Request) string {
	h := r.Header.Get("Authorization")
	if strings.HasPrefix(h, "Bearer ") {
		return strings.TrimPrefix(h, "Bearer ")
	}
	// Também aceitamos via query "?token=" — útil para o WebSocket, que não
	// deixa mandar cabeçalhos customizados facilmente pelo navegador.
	return r.URL.Query().Get("token")
}
