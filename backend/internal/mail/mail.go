// Package mail envia e-mails simples por SMTP. É usado pra mandar as denúncias
// e os reports de suporte pro e-mail da dona do app.
//
// Segurança: a senha aqui deve ser uma "senha de app" do Gmail (gerada com a
// verificação em 2 etapas ligada), NUNCA a senha normal da conta. Ela vem por
// variável de ambiente (MSG_SMTP_PASS), não fica escrita no código.
package mail

import (
	"fmt"
	"net/smtp"
	"strings"
)

type Mailer struct {
	Host string
	Port string
	User string
	Pass string
	To   string
}

// Enabled diz se o envio está configurado. Se não, a gente simplesmente não
// manda e-mail (os reports continuam salvos no banco).
func (m Mailer) Enabled() bool {
	return m.User != "" && m.Pass != "" && m.To != ""
}

// Send envia um e-mail de texto simples. replyTo (opcional) é o endereço que
// aparece no "Responder" — útil pra responder direto quem mandou o report.
func (m Mailer) Send(subject, body, replyTo string) error {
	if !m.Enabled() {
		return nil // não configurado: não faz nada (sem erro)
	}
	addr := m.Host + ":" + m.Port
	auth := smtp.PlainAuth("", m.User, m.Pass, m.Host)

	var b strings.Builder
	fmt.Fprintf(&b, "From: %s\r\n", m.User)
	fmt.Fprintf(&b, "To: %s\r\n", m.To)
	if replyTo != "" {
		fmt.Fprintf(&b, "Reply-To: %s\r\n", replyTo)
	}
	fmt.Fprintf(&b, "Subject: %s\r\n", subject)
	b.WriteString("MIME-Version: 1.0\r\n")
	b.WriteString("Content-Type: text/plain; charset=UTF-8\r\n")
	b.WriteString("\r\n")
	b.WriteString(body)

	return smtp.SendMail(addr, auth, m.User, []string{m.To}, []byte(b.String()))
}
