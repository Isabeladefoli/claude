// Package handlers contém a lógica de cada rota da API HTTP: registrar, logar,
// ver perfil, mandar mensagem, buscar histórico, etc.
package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/mail"
	"github.com/isabeladefoli/private-messenger/backend/internal/ws"
)

// Handlers agrupa as dependências que todos os handlers precisam: o banco, o
// gerenciador de tokens e o hub de WebSocket. Passar tudo por aqui (em vez de
// variáveis globais) deixa o código testável e organizado.
type Handlers struct {
	DB     *sql.DB
	Tokens *auth.TokenManager
	Hub    *ws.Hub
	// Pasta onde os arquivos de mídia são gravados. Preenchida pelo main após
	// o New (mantém a assinatura de New simples e não quebra os testes).
	MediaDir string

	// Envio de e-mail (denúncias/reports). Se não configurado, fica inativo.
	Mailer mail.Mailer
}

func New(db *sql.DB, tokens *auth.TokenManager, hub *ws.Hub) *Handlers {
	return &Handlers{DB: db, Tokens: tokens, Hub: hub}
}

// mediaDir devolve a pasta de mídia, caindo num padrão se não foi configurada.
func (h *Handlers) mediaDir() string {
	if h.MediaDir == "" {
		return "data/media"
	}
	return h.MediaDir
}

// ---- Helpers pra responder em JSON de forma consistente ----

// writeJSON manda uma resposta JSON com o status HTTP dado.
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

// writeError manda um erro no formato { "error": "mensagem" }.
func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

// decodeJSON lê o corpo do pedido para uma struct. Retorna false (e já responde
// o erro) se o JSON estiver malformado.
func decodeJSON(w http.ResponseWriter, r *http.Request, dst interface{}) bool {
	// Limita o corpo a 1 MB pra evitar que alguém mande um JSON gigante e
	// estoure a memória do servidor (mais uma proteçãozinha contra abuso).
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields() // rejeita campos estranhos = pega bugs cedo
	if err := dec.Decode(dst); err != nil {
		writeError(w, http.StatusBadRequest, "JSON inválido: "+err.Error())
		return false
	}
	return true
}

// trimmedOrNil recebe um ponteiro de string (campo opcional do JSON) e devolve:
//   - nil, se o campo veio ausente ou só com espaços (não guardamos "" à toa)
//   - um ponteiro pro texto já sem espaços nas pontas, caso contrário
func trimmedOrNil(s *string) *string {
	if s == nil {
		return nil
	}
	t := strings.TrimSpace(*s)
	if t == "" {
		return nil
	}
	return &t
}
