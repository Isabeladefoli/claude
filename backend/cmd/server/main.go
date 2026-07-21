// Comando "server": o ponto de entrada do backend. Aqui a gente monta todas as
// peças (config, banco, autenticação, websocket, handlers) e liga o servidor.
//
// Pra rodar:  cd backend && go run ./cmd/server
package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/isabeladefoli/private-messenger/backend/internal/auth"
	"github.com/isabeladefoli/private-messenger/backend/internal/config"
	"github.com/isabeladefoli/private-messenger/backend/internal/db"
	"github.com/isabeladefoli/private-messenger/backend/internal/handlers"
	"github.com/isabeladefoli/private-messenger/backend/internal/ratelimit"
	"github.com/isabeladefoli/private-messenger/backend/internal/ws"
)

func main() {
	// 1) Carrega config do ambiente.
	cfg := config.Load()

	// 2) Abre o banco e cria as tabelas.
	database, err := db.Open(cfg.DBPath)
	if err != nil {
		log.Fatalf("banco: %v", err)
	}
	defer database.Close()

	// 3) Monta os componentes.
	tokens := auth.NewTokenManager(cfg.JWTSecret)
	hub := ws.NewHub(tokens)
	h := handlers.New(database, tokens, hub)

	// 4) Define as rotas. Usamos o roteador padrão do Go 1.22+, que já entende
	//    métodos (GET/POST) e parâmetros no caminho ({otherID}).
	mux := http.NewServeMux()

	// Rota de saúde: útil pra checar se o servidor está no ar.
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("ok"))
	})

	// --- Rotas PÚBLICAS (não exigem login) ---
	mux.HandleFunc("POST /api/register", h.Register)
	mux.HandleFunc("POST /api/login", h.Login)

	// --- Rotas PROTEGIDAS (exigem token válido) ---
	// Embrulhamos cada uma com o middleware RequireAuth.
	protected := func(fn http.HandlerFunc) http.Handler {
		return tokens.RequireAuth(fn)
	}
	mux.Handle("GET /api/me", protected(h.Me))
	mux.Handle("GET /api/users/", protected(h.GetUserByUsername)) // /api/users/{username}

	// Mensagens e conversas 1-a-1
	mux.Handle("POST /api/messages", protected(h.SendMessage))
	mux.Handle("GET /api/conversations", protected(h.ListConversations))
	mux.Handle("GET /api/conversations/{otherID}", protected(h.GetConversation))

	// Grupos
	mux.Handle("POST /api/groups", protected(h.CreateGroup))
	mux.Handle("GET /api/groups", protected(h.ListMyGroups))
	mux.Handle("GET /api/groups/{groupID}", protected(h.GetGroup))
	mux.Handle("POST /api/groups/{groupID}/members", protected(h.AddMember))
	mux.Handle("DELETE /api/groups/{groupID}/members/{userID}", protected(h.RemoveMember))
	mux.Handle("POST /api/groups/{groupID}/messages", protected(h.SendGroupMessage))
	mux.Handle("GET /api/groups/{groupID}/messages", protected(h.GetGroupMessages))

	// --- WebSocket (autentica pelo ?token= por dentro) ---
	mux.HandleFunc("GET /ws", hub.ServeWS)

	// 5) Envolve TODO o servidor com o rate limiter (proteção contra abuso).
	//    5 pedidos/seg por IP, tolerando picos de 15.
	limiter := ratelimit.New(5, 15)
	handler := limiter.Middleware(securityHeaders(mux))

	// 6) Configura o servidor HTTP com timeouts. Timeouts evitam que conexões
	//    lentas/abertas de propósito (uma forma de ataque) segurem recursos.
	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	// 7) Sobe o servidor numa rotina e espera sinal de desligamento pra fechar
	//    com elegância (graceful shutdown) — termina o que está em andamento.
	go func() {
		log.Printf("🚀 servidor no ar em %s", cfg.Addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("servidor: %v", err)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)
	<-stop

	log.Println("desligando...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	srv.Shutdown(ctx)
}

// securityHeaders adiciona alguns cabeçalhos de segurança em toda resposta.
// São defesas baratas contra ataques comuns em clientes web.
func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		next.ServeHTTP(w, r)
	})
}
