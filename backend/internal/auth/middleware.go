package auth

import (
	"context"
	"net/http"
)

// ---------------------------------------------------------------------------
// Middleware é uma função que "embrulha" as rotas protegidas. Antes de deixar
// o pedido chegar na lógica de verdade (mandar mensagem, ver histórico), ela:
//   1. pega o token do cabeçalho
//   2. valida o token
//   3. se válido, descobre QUEM é o usuário e guarda o ID no "context"
//   4. se inválido, corta o pedido com 401 (Não Autorizado)
//
// "context" é a forma padrão em Go de passar dados por dentro do pedido HTTP.
// ---------------------------------------------------------------------------

// ctxKey é um tipo privado só pra usar como chave no context sem colidir com
// outras bibliotecas.
type ctxKey string

const userIDKey ctxKey = "userID"

// RequireAuth devolve um middleware que exige um token válido.
func (t *TokenManager) RequireAuth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tokenStr := tokenFromRequest(r)
		if tokenStr == "" {
			http.Error(w, "faltando token de autenticação", http.StatusUnauthorized)
			return
		}

		userID, err := t.Parse(tokenStr)
		if err != nil {
			http.Error(w, "token inválido", http.StatusUnauthorized)
			return
		}

		// Anexa o ID do usuário ao context e segue para a próxima etapa.
		ctx := context.WithValue(r.Context(), userIDKey, userID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// UserIDFromContext recupera o ID do usuário logado de dentro do context.
// Os handlers usam isso pra saber "quem está falando".
func UserIDFromContext(ctx context.Context) (int64, bool) {
	id, ok := ctx.Value(userIDKey).(int64)
	return id, ok
}
