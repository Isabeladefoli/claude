package handlers

import (
	"net/http"
	"strconv"
	"time"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/models"
	"github.com/isabeladefoli/private-messenger/backend/internal/ws"
)

// ---------------------------------------------------------------------------
// Rotas de mensagens 1-a-1.
//
// Fluxo completo de uma mensagem (guarde essa lógica, é o coração do app):
//
//  1. No celular da Ana, o texto "oi" é CRIPTOGRAFADO usando a chave PÚBLICA
//     do Beto. Vira um "ciphertext" embaralhado + um "nonce".
//  2. A Ana manda pro servidor só o ciphertext e o nonce (o servidor NUNCA vê
//     "oi"). É a criptografia ponta-a-ponta (E2E).
//  3. O servidor salva no banco e, se o Beto estiver online, empurra na hora
//     pelo WebSocket. Se estiver offline, fica salvo pro histórico.
//  4. No celular do Beto, o ciphertext é DESCRIPTOGRAFADO com a chave PRIVADA
//     dele (que só ele tem) e vira "oi" de novo.
//
// Resultado: nem o servidor, nem a Cloudflare, nem ninguém no meio lê o "oi".
// ---------------------------------------------------------------------------

// sendMessageRequest é o JSON de envio de mensagem. Repare: não existe campo
// "texto". Só o conteúdo já criptografado.
type sendMessageRequest struct {
	RecipientID int64  `json:"recipient_id"`
	Ciphertext  string `json:"ciphertext"` // texto já criptografado, base64
	Nonce       string `json:"nonce"`      // nonce da criptografia, base64
}

// SendMessage recebe uma mensagem 1-a-1, salva e entrega em tempo real.
func (h *Handlers) SendMessage(w http.ResponseWriter, r *http.Request) {
	senderID, _ := auth.UserIDFromContext(r.Context())

	var req sendMessageRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.RecipientID == 0 || req.Ciphertext == "" || req.Nonce == "" {
		writeError(w, http.StatusBadRequest, "faltam campos: recipient_id, ciphertext, nonce")
		return
	}
	// Confere que o destinatário existe de verdade.
	var exists int
	h.DB.QueryRow(`SELECT 1 FROM users WHERE id = ?`, req.RecipientID).Scan(&exists)
	if exists == 0 {
		writeError(w, http.StatusNotFound, "destinatário não existe")
		return
	}

	// Salva a mensagem (já criptografada) no banco.
	res, err := h.DB.Exec(
		`INSERT INTO messages (sender_id, recipient_id, ciphertext, nonce)
		 VALUES (?, ?, ?, ?)`,
		senderID, req.RecipientID, req.Ciphertext, req.Nonce,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao salvar mensagem")
		return
	}

	msgID, _ := res.LastInsertId()
	msg := models.Message{
		ID:          msgID,
		SenderID:    senderID,
		RecipientID: &req.RecipientID,
		Ciphertext:  req.Ciphertext,
		Nonce:       req.Nonce,
		CreatedAt:   time.Now().UTC(),
	}

	// Tenta entregar em tempo real pelo WebSocket. Se o destinatário estiver
	// offline, tudo bem — a mensagem já está salva e ele pega no histórico.
	if payload, err := ws.Pack("message", msg); err == nil {
		h.Hub.SendToUser(req.RecipientID, payload)
		// Também avisa os OUTROS aparelhos do remetente (multi-device): sem
		// isso, se a Isa manda mensagem pelo celular, o PC dela (mesma conta)
		// só saberia dessa conversa nova ao reabrir o app.
		h.Hub.SendToUser(senderID, payload)
	}

	writeJSON(w, http.StatusCreated, msg)
}

// GetConversation devolve o histórico de mensagens entre o usuário logado e
// outro usuário. Suporta paginação por "before" (id) pra carregar mensagens
// antigas conforme rola a tela pra cima.
func (h *Handlers) GetConversation(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	// /api/conversations/{otherID}
	otherStr := r.PathValue("otherID")
	otherID, err := strconv.ParseInt(otherStr, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "id do outro usuário inválido")
		return
	}

	// Parâmetros opcionais de paginação: ?limit=50&before=<id>
	limit := 50
	if l := r.URL.Query().Get("limit"); l != "" {
		if n, err := strconv.Atoi(l); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	before := int64(1<<62 - 1) // "antes de tudo" = pega as mais recentes
	if b := r.URL.Query().Get("before"); b != "" {
		if n, err := strconv.ParseInt(b, 10, 64); err == nil {
			before = n
		}
	}

	// Busca mensagens onde (eu->ele) OU (ele->eu). Só as duas pontas da conversa.
	rows, err := h.DB.Query(
		`SELECT id, sender_id, recipient_id, ciphertext, nonce, created_at
		 FROM messages
		 WHERE ((sender_id = ? AND recipient_id = ?)
		     OR (sender_id = ? AND recipient_id = ?))
		   AND id < ?
		 ORDER BY id DESC
		 LIMIT ?`,
		userID, otherID, otherID, userID, before, limit,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar conversa")
		return
	}
	defer rows.Close()

	messages := make([]models.Message, 0, limit)
	for rows.Next() {
		var m models.Message
		if err := rows.Scan(&m.ID, &m.SenderID, &m.RecipientID,
			&m.Ciphertext, &m.Nonce, &m.CreatedAt); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler mensagens")
			return
		}
		messages = append(messages, m)
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"messages": messages,
	})
}
