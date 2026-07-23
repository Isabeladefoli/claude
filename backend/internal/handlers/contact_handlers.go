package handlers

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/models"
)

// ---------------------------------------------------------------------------
// CONTATOS e a LISTA DE CHATS
//
// A ideia: a tela inicial mostra "chats". Um chat pode ser:
//   - alguém com quem você já trocou mensagens (tem histórico),
//   - e/ou um contato SALVO (adicionado de propósito, mesmo sem mensagens).
//
// Cada chat carrega dois marcadores:
//   - saved:  foi salvo/adicionado (aparece no filtro "Salvos")
//   - hidden: foi apagado da lista "Todos" pra deixar limpo (mas continua
//             existindo — ainda aparece em "Salvos" e na busca).
//
// O backend só DEVOLVE esses marcadores. Quem decide o que mostrar em cada
// filtro (Todos / Salvos / Não salvos) e como a busca funciona é o APP —
// assim a filtragem é instantânea, sem ida e volta ao servidor.
// ---------------------------------------------------------------------------

// publicUser é o "cartão" público de um usuário na lista de chats.
type publicUser struct {
	ID       int64   `json:"id"`
	Username string  `json:"username"`
	Avatar   *string `json:"avatar_url,omitempty"`
}

// chatItem é uma linha da lista de chats.
type chatItem struct {
	User        publicUser      `json:"user"`
	Saved       bool            `json:"saved"`
	Hidden      bool            `json:"hidden"`
	HasMessages bool            `json:"has_messages"`
	UnreadCount int             `json:"unread_count"` // mensagens não-lidas do outro
	LastMessage *models.Message `json:"last_message,omitempty"`
}

// AddContact salva/adiciona um contato pelo nome de usuário. Depois disso ele
// aparece em "Salvos" e em "Todos", mesmo sem nenhuma mensagem trocada.
func (h *Handlers) AddContact(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())

	var req struct {
		Username string `json:"username"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	req.Username = strings.TrimSpace(req.Username)
	if req.Username == "" {
		writeError(w, http.StatusBadRequest, "informe o nome de usuário")
		return
	}

	// Acha o usuário alvo.
	var target models.User
	err := h.DB.QueryRow(
		`SELECT id, username, public_key, avatar_url, created_at
		 FROM users WHERE username = ?`,
		req.Username,
	).Scan(&target.ID, &target.Username, &target.PublicKey, &target.AvatarURL, &target.CreatedAt)
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusNotFound, "usuário não encontrado")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao buscar usuário")
		return
	}

	// Salva (ou re-salva) o contato. Se já existia escondido, ao adicionar de
	// novo ele volta a aparecer (hidden = 0).
	_, err = h.DB.Exec(
		`INSERT INTO contacts (owner_id, contact_id, saved, hidden)
		 VALUES (?, ?, 1, 0)
		 ON CONFLICT(owner_id, contact_id) DO UPDATE SET saved = 1, hidden = 0`,
		ownerID, target.ID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao salvar contato")
		return
	}

	writeJSON(w, http.StatusCreated, publicUser{
		ID:       target.ID,
		Username: target.Username,
		Avatar:   target.AvatarURL,
	})
}

// HideChat "apaga" um chat da lista Todos (marca hidden = 1). Ele NÃO some de
// verdade: continua em Salvos (se for salvo) e continua achável na busca.
func (h *Handlers) HideChat(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "otherID")
	if !ok {
		return
	}
	_, err := h.DB.Exec(
		`INSERT INTO contacts (owner_id, contact_id, saved, hidden)
		 VALUES (?, ?, 0, 1)
		 ON CONFLICT(owner_id, contact_id) DO UPDATE SET hidden = 1`,
		ownerID, otherID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao apagar chat")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// UnhideChat desfaz o HideChat (hidden = 0). Serve pro "reverter".
func (h *Handlers) UnhideChat(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "otherID")
	if !ok {
		return
	}
	_, err := h.DB.Exec(
		`UPDATE contacts SET hidden = 0 WHERE owner_id = ? AND contact_id = ?`,
		ownerID, otherID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao restaurar chat")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// RemoveContact "desfaz o salvar" de um contato (saved = 0): ele sai de
// "Salvos". Se ainda houver conversa, o chat continua existindo em "Todos".
func (h *Handlers) RemoveContact(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "otherID")
	if !ok {
		return
	}
	_, err := h.DB.Exec(
		`UPDATE contacts SET saved = 0 WHERE owner_id = ? AND contact_id = ?`,
		ownerID, otherID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao excluir contato")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// ListChats devolve a lista unificada: todo mundo com quem você conversou +
// todos os seus contatos salvos, cada um com seus marcadores e a última
// mensagem (se houver).
func (h *Handlers) ListChats(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	// partners: o "outro lado" de cada conversa 1-a-1 + id da última mensagem.
	// ids: todos os usuários relevantes = parceiros de conversa UNIÃO contatos.
	// Depois juntamos com users (dados), contacts (marcadores) e a última msg.
	query := `
		WITH partners AS (
			SELECT
				CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END AS uid,
				MAX(id) AS last_id
			FROM messages
			WHERE group_id IS NULL AND (sender_id = ? OR recipient_id = ?)
			GROUP BY uid
		),
		ids AS (
			SELECT uid FROM partners
			UNION
			SELECT contact_id AS uid FROM contacts WHERE owner_id = ?
		)
		SELECT
			u.id, u.username, u.avatar_url,
			COALESCE(c.saved, 0)  AS saved,
			COALESCE(c.hidden, 0) AS hidden,
			(SELECT COUNT(*) FROM messages mm
			   WHERE mm.group_id IS NULL AND mm.sender_id = ids.uid
			     AND mm.recipient_id = ?
			     AND mm.id > COALESCE(cr.last_read_id, 0)) AS unread,
			m.id, m.sender_id, m.recipient_id, m.ciphertext, m.nonce, m.created_at
		FROM ids
		JOIN users u          ON u.id = ids.uid
		LEFT JOIN partners p    ON p.uid = ids.uid
		LEFT JOIN contacts c    ON c.owner_id = ? AND c.contact_id = ids.uid
		LEFT JOIN chat_reads cr ON cr.owner_id = ? AND cr.other_id = ids.uid
		LEFT JOIN messages m    ON m.id = p.last_id
		ORDER BY (m.id IS NULL), m.id DESC, u.username COLLATE NOCASE
	`
	rows, err := h.DB.Query(query, userID, userID, userID, userID, userID, userID, userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao listar chats")
		return
	}
	defer rows.Close()

	chats := make([]chatItem, 0)
	for rows.Next() {
		var it chatItem
		var savedInt, hiddenInt, unread int

		// Campos da última mensagem podem ser NULL (contato sem conversa).
		var mID, mSender sql.NullInt64
		var mRecipient sql.NullInt64
		var mCipher, mNonce sql.NullString
		var mCreated sql.NullTime

		if err := rows.Scan(
			&it.User.ID, &it.User.Username, &it.User.Avatar,
			&savedInt, &hiddenInt, &unread,
			&mID, &mSender, &mRecipient, &mCipher, &mNonce, &mCreated,
		); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler chats")
			return
		}

		it.Saved = savedInt == 1
		it.Hidden = hiddenInt == 1
		it.UnreadCount = unread
		if mID.Valid {
			it.HasMessages = true
			rid := mRecipient.Int64
			msg := &models.Message{
				ID:         mID.Int64,
				SenderID:   mSender.Int64,
				Ciphertext: mCipher.String,
				Nonce:      mNonce.String,
				CreatedAt:  mCreated.Time,
			}
			if mRecipient.Valid {
				msg.RecipientID = &rid
			}
			it.LastMessage = msg
		}
		chats = append(chats, it)
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{"chats": chats})
}

// MarkChatRead marca a conversa com "other" como lida até a última mensagem.
// Depois disso, o contador de não-lidas daquele chat zera.
func (h *Handlers) MarkChatRead(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())
	otherID, ok := pathID(w, r, "otherID")
	if !ok {
		return
	}
	var maxID sql.NullInt64
	h.DB.QueryRow(
		`SELECT MAX(id) FROM messages
		 WHERE group_id IS NULL AND ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))`,
		ownerID, otherID, otherID, ownerID,
	).Scan(&maxID)
	id := int64(0)
	if maxID.Valid {
		id = maxID.Int64
	}
	_, err := h.DB.Exec(
		`INSERT INTO chat_reads (owner_id, other_id, last_read_id) VALUES (?, ?, ?)
		 ON CONFLICT(owner_id, other_id) DO UPDATE SET last_read_id = ?`,
		ownerID, otherID, id, id,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao marcar como lida")
		return
	}
	writeJSON(w, http.StatusOK, map[string]bool{"ok": true})
}

// pathID lê um parâmetro de caminho numérico (ex: {otherID}) e já responde erro
// se estiver ausente/ inválido.
func pathID(w http.ResponseWriter, r *http.Request, name string) (int64, bool) {
	raw := r.PathValue(name)
	id, err := strconv.ParseInt(raw, 10, 64)
	if err != nil || id <= 0 {
		writeError(w, http.StatusBadRequest, "id inválido")
		return 0, false
	}
	return id, true
}
