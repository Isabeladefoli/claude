package handlers

import (
	"net/http"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/models"
)

// ---------------------------------------------------------------------------
// Lista de conversas — o que aparece na TELA INICIAL do app: cada pessoa com
// quem você trocou mensagens, junto da última mensagem trocada.
//
// A parte "difícil" aqui é de SQL: pra cada parceiro de conversa, queremos só
// a mensagem MAIS RECENTE. Fazemos isso pegando o maior id de mensagem por
// parceiro e juntando de volta com a tabela de mensagens.
// ---------------------------------------------------------------------------

// conversationSummary resume uma conversa 1-a-1 pra lista.
type conversationSummary struct {
	PartnerID       int64          `json:"partner_id"`
	PartnerUsername string         `json:"partner_username"`
	PartnerAvatar   *string        `json:"partner_avatar,omitempty"`
	LastMessage     models.Message `json:"last_message"`
}

// ListConversations devolve, pro usuário logado, o resumo de cada conversa
// 1-a-1, ordenado da mais recente pra mais antiga.
func (h *Handlers) ListConversations(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	// Passo 1 (subconsulta "last"): pra cada "parceiro", o id da última mensagem.
	//   - o parceiro é o OUTRO lado da conversa: se eu sou o sender, é o
	//     recipient; se eu sou o recipient, é o sender.
	//   - só mensagens 1-a-1 (group_id IS NULL).
	// Passo 2: junta esse id de volta com a mensagem completa e com o usuário
	//   parceiro pra pegar username e avatar.
	query := `
		WITH last AS (
			SELECT
				CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END AS partner_id,
				MAX(id) AS last_id
			FROM messages
			WHERE group_id IS NULL
			  AND (sender_id = ? OR recipient_id = ?)
			GROUP BY partner_id
		)
		SELECT
			l.partner_id, u.username, u.avatar_url,
			m.id, m.sender_id, m.recipient_id, m.ciphertext, m.nonce, m.created_at
		FROM last l
		JOIN messages m ON m.id = l.last_id
		JOIN users u    ON u.id = l.partner_id
		ORDER BY m.id DESC
	`

	rows, err := h.DB.Query(query, userID, userID, userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao listar conversas")
		return
	}
	defer rows.Close()

	conversations := make([]conversationSummary, 0)
	for rows.Next() {
		var c conversationSummary
		var m models.Message
		if err := rows.Scan(
			&c.PartnerID, &c.PartnerUsername, &c.PartnerAvatar,
			&m.ID, &m.SenderID, &m.RecipientID, &m.Ciphertext, &m.Nonce, &m.CreatedAt,
		); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler conversas")
			return
		}
		c.LastMessage = m
		conversations = append(conversations, c)
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{"conversations": conversations})
}
