package handlers

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/models"
	"github.com/isabeladefoli/private-messenger/backend/internal/ws"
)

// ---------------------------------------------------------------------------
// GRUPOS
//
// A LÓGICA da criptografia E2E em grupo (é diferente do 1-a-1!):
//
// No 1-a-1 é fácil: a Ana criptografa com a chave pública do Beto, pronto.
// Num grupo de 10 pessoas, criptografar 10 vezes (uma por pessoa) a cada
// mensagem seria caro e lento.
//
// A solução (mesma do WhatsApp/Signal) chama-se "sender key" (chave de
// remetente): cada membro cria UMA chave simétrica secreta e a distribui aos
// outros membros usando o canal 1-a-1 já criptografado. Depois, ele criptografa
// cada mensagem UMA vez com essa chave, e todos conseguem abrir.
//
// O ponto importante pro BACKEND: ele continua "burro". Ele só:
//   - guarda quem está em qual grupo,
//   - recebe UM envelope já fechado (ciphertext) por mensagem,
//   - distribui (fan-out) esse envelope pra todos os membros.
// A distribuição das sender keys acontece toda no APP (Fase 2), reaproveitando
// o mecanismo 1-a-1. O servidor nunca vê chave nem texto. 🔒
// ---------------------------------------------------------------------------

// isGroupMember confere se um usuário pertence a um grupo. Base de toda a
// autorização de grupo: só membros leem e postam.
func (h *Handlers) isGroupMember(groupID, userID int64) (bool, error) {
	var one int
	err := h.DB.QueryRow(
		`SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?`,
		groupID, userID,
	).Scan(&one)
	if errors.Is(err, sql.ErrNoRows) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return true, nil
}

// groupIDFromPath extrai o {groupID} da rota e valida.
func groupIDFromPath(r *http.Request) (int64, bool) {
	id, err := strconv.ParseInt(r.PathValue("groupID"), 10, 64)
	if err != nil || id <= 0 {
		return 0, false
	}
	return id, true
}

// --- Criar grupo ---

type createGroupRequest struct {
	Name string `json:"name"`
}

// CreateGroup cria um grupo novo. Quem cria vira dono e já entra como membro.
func (h *Handlers) CreateGroup(w http.ResponseWriter, r *http.Request) {
	ownerID, _ := auth.UserIDFromContext(r.Context())

	var req createGroupRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	if req.Name == "" || len(req.Name) > 60 {
		writeError(w, http.StatusBadRequest, "nome do grupo deve ter de 1 a 60 caracteres")
		return
	}

	// Usamos uma transação: OU as duas inserções acontecem (grupo + o dono como
	// membro), OU nenhuma. Evita um grupo "órfão" sem membros se algo falhar.
	tx, err := h.DB.Begin()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao criar grupo")
		return
	}
	defer tx.Rollback() // se der commit, o rollback vira no-op

	res, err := tx.Exec(`INSERT INTO groups (name, owner_id) VALUES (?, ?)`, req.Name, ownerID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao criar grupo")
		return
	}
	groupID, _ := res.LastInsertId()

	if _, err := tx.Exec(
		`INSERT INTO group_members (group_id, user_id) VALUES (?, ?)`,
		groupID, ownerID,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao adicionar dono ao grupo")
		return
	}

	if err := tx.Commit(); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao finalizar criação")
		return
	}

	writeJSON(w, http.StatusCreated, models.Group{
		ID:        groupID,
		Name:      req.Name,
		OwnerID:   ownerID,
		CreatedAt: time.Now().UTC(),
	})
}

// --- Listar meus grupos ---

// ListMyGroups devolve os grupos onde o usuário logado é membro.
func (h *Handlers) ListMyGroups(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())

	rows, err := h.DB.Query(
		`SELECT g.id, g.name, g.owner_id, g.created_at
		 FROM groups g
		 JOIN group_members m ON m.group_id = g.id
		 WHERE m.user_id = ?
		 ORDER BY g.created_at DESC`,
		userID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao listar grupos")
		return
	}
	defer rows.Close()

	groups := make([]models.Group, 0)
	for rows.Next() {
		var g models.Group
		if err := rows.Scan(&g.ID, &g.Name, &g.OwnerID, &g.CreatedAt); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler grupos")
			return
		}
		groups = append(groups, g)
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"groups": groups})
}

// --- Ver detalhes + membros de um grupo ---

// GetGroup devolve os dados do grupo e a lista de membros (só pra membros).
func (h *Handlers) GetGroup(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())
	groupID, ok := groupIDFromPath(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "id de grupo inválido")
		return
	}

	member, err := h.isGroupMember(groupID, userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro de autorização")
		return
	}
	if !member {
		// 404 (e não 403) de propósito: não confirmamos nem que o grupo existe
		// pra quem não é membro. Menos informação vaza pra quem bisbilhota.
		writeError(w, http.StatusNotFound, "grupo não encontrado")
		return
	}

	var g models.Group
	if err := h.DB.QueryRow(
		`SELECT id, name, owner_id, created_at FROM groups WHERE id = ?`, groupID,
	).Scan(&g.ID, &g.Name, &g.OwnerID, &g.CreatedAt); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar grupo")
		return
	}

	// Carrega os membros (id, username e chave pública — o app precisa das
	// chaves pra distribuir a "sender key" da criptografia de grupo).
	rows, err := h.DB.Query(
		`SELECT u.id, u.username, u.public_key
		 FROM users u JOIN group_members m ON m.user_id = u.id
		 WHERE m.group_id = ?`,
		groupID,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar membros")
		return
	}
	defer rows.Close()

	type groupMember struct {
		ID        int64  `json:"id"`
		Username  string `json:"username"`
		PublicKey string `json:"public_key"`
	}
	members := make([]groupMember, 0)
	for rows.Next() {
		var m groupMember
		if err := rows.Scan(&m.ID, &m.Username, &m.PublicKey); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler membros")
			return
		}
		members = append(members, m)
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"group":   g,
		"members": members,
	})
}

// --- Adicionar membro ---

type addMemberRequest struct {
	UserID int64 `json:"user_id"`
}

// AddMember adiciona alguém ao grupo. Só o DONO pode adicionar.
func (h *Handlers) AddMember(w http.ResponseWriter, r *http.Request) {
	actorID, _ := auth.UserIDFromContext(r.Context())
	groupID, ok := groupIDFromPath(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "id de grupo inválido")
		return
	}

	var req addMemberRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.UserID <= 0 {
		writeError(w, http.StatusBadRequest, "user_id inválido")
		return
	}

	// Só o dono adiciona membros.
	var ownerID int64
	err := h.DB.QueryRow(`SELECT owner_id FROM groups WHERE id = ?`, groupID).Scan(&ownerID)
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusNotFound, "grupo não encontrado")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao verificar grupo")
		return
	}
	if ownerID != actorID {
		writeError(w, http.StatusForbidden, "só o dono do grupo pode adicionar membros")
		return
	}

	// Confere que o usuário a adicionar existe.
	var exists int
	h.DB.QueryRow(`SELECT 1 FROM users WHERE id = ?`, req.UserID).Scan(&exists)
	if exists == 0 {
		writeError(w, http.StatusNotFound, "usuário a adicionar não existe")
		return
	}

	// "INSERT OR IGNORE": se já for membro, não faz nada (sem erro).
	if _, err := h.DB.Exec(
		`INSERT OR IGNORE INTO group_members (group_id, user_id) VALUES (?, ?)`,
		groupID, req.UserID,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao adicionar membro")
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"status": "membro adicionado"})
}

// --- Sair / remover membro ---

// RemoveMember tira um membro do grupo. O dono pode remover qualquer um; um
// membro comum só pode remover a si mesmo (sair do grupo).
func (h *Handlers) RemoveMember(w http.ResponseWriter, r *http.Request) {
	actorID, _ := auth.UserIDFromContext(r.Context())
	groupID, ok := groupIDFromPath(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "id de grupo inválido")
		return
	}
	targetID, err := strconv.ParseInt(r.PathValue("userID"), 10, 64)
	if err != nil || targetID <= 0 {
		writeError(w, http.StatusBadRequest, "id de usuário inválido")
		return
	}

	var ownerID int64
	err = h.DB.QueryRow(`SELECT owner_id FROM groups WHERE id = ?`, groupID).Scan(&ownerID)
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, http.StatusNotFound, "grupo não encontrado")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao verificar grupo")
		return
	}

	// Autorização: é o dono, OU está saindo por conta própria.
	if actorID != ownerID && actorID != targetID {
		writeError(w, http.StatusForbidden, "sem permissão pra remover esse membro")
		return
	}
	// Regra simples: o dono não pode se remover (senão o grupo fica sem dono).
	if targetID == ownerID {
		writeError(w, http.StatusBadRequest, "o dono não pode sair do próprio grupo")
		return
	}

	if _, err := h.DB.Exec(
		`DELETE FROM group_members WHERE group_id = ? AND user_id = ?`,
		groupID, targetID,
	); err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao remover membro")
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"status": "membro removido"})
}

// --- Mensagens de grupo ---

type sendGroupMessageRequest struct {
	Ciphertext string `json:"ciphertext"`
	Nonce      string `json:"nonce"`
}

// SendGroupMessage salva uma mensagem de grupo e a distribui (fan-out) pra
// todos os outros membros que estiverem online.
func (h *Handlers) SendGroupMessage(w http.ResponseWriter, r *http.Request) {
	senderID, _ := auth.UserIDFromContext(r.Context())
	groupID, ok := groupIDFromPath(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "id de grupo inválido")
		return
	}

	// Só membros podem postar.
	member, err := h.isGroupMember(groupID, senderID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro de autorização")
		return
	}
	if !member {
		writeError(w, http.StatusNotFound, "grupo não encontrado")
		return
	}

	var req sendGroupMessageRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.Ciphertext == "" || req.Nonce == "" {
		writeError(w, http.StatusBadRequest, "faltam campos: ciphertext, nonce")
		return
	}

	res, err := h.DB.Exec(
		`INSERT INTO messages (sender_id, group_id, ciphertext, nonce) VALUES (?, ?, ?, ?)`,
		senderID, groupID, req.Ciphertext, req.Nonce,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao salvar mensagem")
		return
	}
	msgID, _ := res.LastInsertId()

	msg := models.Message{
		ID:         msgID,
		SenderID:   senderID,
		GroupID:    &groupID,
		Ciphertext: req.Ciphertext,
		Nonce:      req.Nonce,
		CreatedAt:  time.Now().UTC(),
	}

	// Fan-out: entrega em tempo real pra todo membro online, menos o remetente.
	h.fanOutToGroup(groupID, senderID, msg)

	writeJSON(w, http.StatusCreated, msg)
}

// fanOutToGroup empurra a mensagem pra todos os membros online (exceto quem
// enviou, que já tem a mensagem na tela dele).
func (h *Handlers) fanOutToGroup(groupID, senderID int64, msg models.Message) {
	rows, err := h.DB.Query(`SELECT user_id FROM group_members WHERE group_id = ?`, groupID)
	if err != nil {
		return // falha no tempo real não é fatal: a mensagem já está salva
	}
	defer rows.Close()

	payload, err := ws.Pack("group_message", msg)
	if err != nil {
		return
	}
	for rows.Next() {
		var memberID int64
		if err := rows.Scan(&memberID); err != nil {
			continue
		}
		if memberID == senderID {
			continue
		}
		h.Hub.SendToUser(memberID, payload)
	}
}

// GetGroupMessages devolve o histórico de mensagens de um grupo (só pra membros).
func (h *Handlers) GetGroupMessages(w http.ResponseWriter, r *http.Request) {
	userID, _ := auth.UserIDFromContext(r.Context())
	groupID, ok := groupIDFromPath(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "id de grupo inválido")
		return
	}

	member, err := h.isGroupMember(groupID, userID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro de autorização")
		return
	}
	if !member {
		writeError(w, http.StatusNotFound, "grupo não encontrado")
		return
	}

	limit := 50
	if l := r.URL.Query().Get("limit"); l != "" {
		if n, err := strconv.Atoi(l); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	before := int64(1<<62 - 1)
	if b := r.URL.Query().Get("before"); b != "" {
		if n, err := strconv.ParseInt(b, 10, 64); err == nil {
			before = n
		}
	}

	rows, err := h.DB.Query(
		`SELECT id, sender_id, group_id, ciphertext, nonce, created_at
		 FROM messages
		 WHERE group_id = ? AND id < ?
		 ORDER BY id DESC LIMIT ?`,
		groupID, before, limit,
	)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "erro ao carregar mensagens")
		return
	}
	defer rows.Close()

	messages := make([]models.Message, 0, limit)
	for rows.Next() {
		var m models.Message
		if err := rows.Scan(&m.ID, &m.SenderID, &m.GroupID,
			&m.Ciphertext, &m.Nonce, &m.CreatedAt); err != nil {
			writeError(w, http.StatusInternalServerError, "erro ao ler mensagens")
			return
		}
		messages = append(messages, m)
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{"messages": messages})
}
