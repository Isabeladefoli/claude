package handlers

import (
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// ---------------------------------------------------------------------------
// Rota de suporte (tela "Support"): o usuário escreve o e-mail dele pra resposta,
// um título e o texto do report. A gente guarda no banco (tabela support_reports)
// pra a dona do app ler depois.
//
// Obs: aqui só ARMAZENAMOS o report. Mandar de fato um e-mail pro
// isabeladefoli@gmail.com precisaria de um servidor SMTP configurado (usuário +
// senha de app do Gmail, por exemplo). Isso fica pra um passo futuro; por ora,
// os reports ficam salvos e dá pra ler direto no banco.
// ---------------------------------------------------------------------------

type supportRequest struct {
	ReplyEmail string `json:"reply_email"` // e-mail que o usuário quer que a gente responda
	Title      string `json:"title"`
	Body       string `json:"body"`
}

// CreateSupportReport valida e guarda um report de suporte.
func (h *Handlers) CreateSupportReport(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	var req supportRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	req.ReplyEmail = strings.TrimSpace(req.ReplyEmail)
	req.Title = strings.TrimSpace(req.Title)
	req.Body = strings.TrimSpace(req.Body)

	// Validações simples: os três campos são obrigatórios e o e-mail precisa ao
	// menos parecer um e-mail (ter "@" e um ".").
	if req.ReplyEmail == "" || !strings.Contains(req.ReplyEmail, "@") || !strings.Contains(req.ReplyEmail, ".") {
		writeError(w, http.StatusBadRequest, "informe um e-mail válido pra resposta")
		return
	}
	if req.Title == "" {
		writeError(w, http.StatusBadRequest, "informe um título")
		return
	}
	if req.Body == "" {
		writeError(w, http.StatusBadRequest, "escreva o report")
		return
	}

	_, err := h.DB.Exec(
		`INSERT INTO support_reports (user_id, reply_email, title, body) VALUES (?, ?, ?, ?)`,
		userID, req.ReplyEmail, req.Title, req.Body,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao enviar report")
		return
	}

	// Manda por e-mail (se configurado). Reply-To é o e-mail que a pessoa deixou,
	// então dá pra responder direto pra ela.
	go func() {
		subject := "Novo report de suporte: " + req.Title
		body := fmt.Sprintf("Report de suporte.\n\nDe: %s\nTítulo: %s\n\n%s\n",
			req.ReplyEmail, req.Title, req.Body)
		if err := h.Mailer.Send(subject, body, req.ReplyEmail); err != nil {
			log.Printf("falha ao enviar e-mail de suporte: %v", err)
		}
	}()

	writeJSON(w, http.StatusCreated, map[string]bool{"ok": true})
}
