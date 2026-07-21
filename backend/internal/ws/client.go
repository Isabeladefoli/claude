package ws

import (
	"time"

	"github.com/gorilla/websocket"
)

// Este arquivo cuida das duas "bombas" (pumps) de cada conexão:
//   - readPump: fica lendo o que o cliente envia
//   - writePump: fica enviando o que temos pra ele
// Rodam em paralelo enquanto a conexão viver.

const (
	// Se o cliente não responder um "ping" em até pongWait, consideramos a
	// conexão morta e fechamos. Isso limpa conexões de gente que caiu a internet.
	pongWait   = 60 * time.Second
	pingPeriod = (pongWait * 9) / 10 // mandamos ping um pouco antes de expirar
	writeWait  = 10 * time.Second
	maxMessage = 8192 // tamanho máx. de uma mensagem recebida, em bytes
)

// readPump lê mensagens vindas do cliente. No nosso desenho, o envio de
// mensagens de chat acontece pela API HTTP (mais simples e confiável), então
// aqui a leitura serve principalmente pra manter a conexão viva (pongs) e, no
// futuro, receber sinais como "está digitando...".
func (c *Client) readPump() {
	defer func() {
		c.hub.unregister(c)
		c.conn.Close()
	}()

	c.conn.SetReadLimit(maxMessage)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	// Toda vez que o cliente responde um pong, renovamos o prazo.
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, _, err := c.conn.ReadMessage()
		if err != nil {
			// Erro de leitura = conexão caiu ou fechou. Encerramos.
			break
		}
		// (Espaço para tratar sinais do cliente no futuro, ex: "typing".)
	}
}

// writePump envia pro cliente tudo que aparecer no canal c.send, e também
// dispara pings periódicos pra checar se ele ainda está vivo.
func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.conn.Close()
	}()

	for {
		select {
		case payload, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				// O canal foi fechado (cliente removido do hub). Avisa e sai.
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.conn.WriteMessage(websocket.TextMessage, payload); err != nil {
				return
			}

		case <-ticker.C:
			// Hora do ping. Se falhar, a conexão morreu.
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}
