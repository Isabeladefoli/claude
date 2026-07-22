// Package ws implementa a comunicação em TEMPO REAL via WebSocket.
//
// ---------------------------------------------------------------------------
// A LÓGICA do tempo real:
//
// HTTP normal é "pergunta e resposta": o cliente pergunta, o servidor responde,
// e a conexão fecha. Ruim pra chat — o servidor não tem como "empurrar" uma
// mensagem nova pro celular sem ele perguntar toda hora.
//
// WebSocket resolve isso: é uma conexão que fica ABERTA nos dois sentidos.
// O celular conecta uma vez e a conexão permanece. Quando chega mensagem pra
// ele, o servidor empurra na hora pela conexão já aberta.
//
// O "Hub" é o cérebro: ele sabe quais usuários estão online e por qual conexão
// falar com cada um. Quando alguém manda mensagem pro usuário 42, o Hub procura
// a conexão do 42 e entrega.
// ---------------------------------------------------------------------------
package ws

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
)

// upgrader transforma uma conexão HTTP normal numa conexão WebSocket.
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	// CheckOrigin decide de quais sites aceitamos conexão. Como o cliente é um
	// app nativo (não um site), liberamos. Se um dia tiver versão web, aqui é
	// onde você restringe aos seus domínios pra evitar conexões maliciosas.
	CheckOrigin: func(r *http.Request) bool { return true },
}

// Client é um usuário conectado: sua conexão + um canal de saída de mensagens.
type Client struct {
	userID int64
	conn   *websocket.Conn
	send   chan []byte // fila de mensagens esperando pra serem enviadas a ele
	hub    *Hub
}

// Hub mantém o mapa de todos os clientes online. Um usuário pode ter MAIS DE
// UMA conexão ao mesmo tempo (ex: celular + emulador, ou dois aparelhos com a
// mesma conta) — por isso o valor é um conjunto de conexões, não uma só.
type Hub struct {
	mu      sync.RWMutex
	clients map[int64]map[*Client]struct{} // userID -> conjunto de conexões dele
	tokens  *auth.TokenManager
}

func NewHub(tokens *auth.TokenManager) *Hub {
	return &Hub{
		clients: make(map[int64]map[*Client]struct{}),
		tokens:  tokens,
	}
}

// register e unregister adicionam/removem clientes do mapa com segurança.
func (h *Hub) register(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	set, ok := h.clients[c.userID]
	if !ok {
		set = make(map[*Client]struct{})
		h.clients[c.userID] = set
	}
	set[c] = struct{}{}
	log.Printf("usuário %d conectou (%d conexão(ões) dele)", c.userID, len(set))
}

func (h *Hub) unregister(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	set, ok := h.clients[c.userID]
	if !ok {
		return
	}
	if _, present := set[c]; !present {
		return
	}
	delete(set, c)
	close(c.send)
	if len(set) == 0 {
		delete(h.clients, c.userID)
	}
	log.Printf("usuário %d desconectou (%d conexão(ões) restante(s))", c.userID, len(set))
}

// SendToUser entrega um pacote (bytes JSON) para TODAS as conexões abertas de
// um usuário (pode estar logado em vários aparelhos). Retorna true se
// conseguiu entregar em pelo menos uma. Se estiver offline em todas, retorna
// false — a mensagem já foi salva no banco e ele pega o histórico quando
// conectar.
func (h *Hub) SendToUser(userID int64, payload []byte) bool {
	h.mu.RLock()
	set := h.clients[userID]
	conns := make([]*Client, 0, len(set))
	for c := range set {
		conns = append(conns, c)
	}
	h.mu.RUnlock()

	delivered := false
	for _, c := range conns {
		// Envio não-bloqueante: se a fila de algum cliente estiver cheia
		// (conexão lenta), não travamos o servidor inteiro por causa dele.
		select {
		case c.send <- payload:
			delivered = true
		default:
		}
	}
	return delivered
}

// IsOnline diz se um usuário está conectado agora (em pelo menos um aparelho).
func (h *Hub) IsOnline(userID int64) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients[userID]) > 0
}

// ServeWS é o handler da rota do WebSocket. Autentica pelo token e sobe a conexão.
func (h *Hub) ServeWS(w http.ResponseWriter, r *http.Request) {
	// O token vem em "?token=..." porque o WebSocket do navegador não deixa
	// mandar cabeçalho Authorization facilmente.
	token := r.URL.Query().Get("token")
	userID, err := h.tokens.Parse(token)
	if err != nil {
		http.Error(w, "token inválido", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("falha ao subir websocket: %v", err)
		return
	}

	client := &Client{
		userID: userID,
		conn:   conn,
		send:   make(chan []byte, 32), // fila de até 32 mensagens
		hub:    h,
	}
	h.register(client)

	// Cada conexão roda duas rotinas: uma pra ler o que o cliente manda, outra
	// pra escrever o que temos pra ele. Elas rodam em paralelo.
	go client.writePump()
	go client.readPump()
}

// Envelope é o formato dos pacotes que trafegam pelo WebSocket. "Type" diz o
// que é (ex: "message", "typing", "receipt") e "Data" carrega o conteúdo.
type Envelope struct {
	Type string          `json:"type"`
	Data json.RawMessage `json:"data"`
}

// Broadcast monta um envelope e devolve os bytes prontos pra enviar.
func Pack(msgType string, data interface{}) ([]byte, error) {
	raw, err := json.Marshal(data)
	if err != nil {
		return nil, err
	}
	return json.Marshal(Envelope{Type: msgType, Data: raw})
}
