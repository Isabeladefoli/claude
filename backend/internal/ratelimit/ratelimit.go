// Package ratelimit limita quantos pedidos cada IP pode fazer por segundo.
//
// ---------------------------------------------------------------------------
// Sobre proteção contra DDoS — a verdade honesta:
//
// Um ataque DDoS de verdade (milhares de máquinas mandando tráfego) NÃO se
// bloqueia dentro do seu servidor Go — quando o pacote chega até aqui, o
// estrago já está feito (sua internet caseira já entupiu). A defesa real
// contra DDoS grande é a CAMADA DE CIMA: o Cloudflare, que fica na frente do
// seu servidor e absorve/filtra o ataque antes de chegar em você. Por isso a
// escolha do Cloudflare Tunnel é o que de fato te protege.
//
// O que ESTE código faz é a defesa da camada de aplicação: impedir que UM
// cliente abusivo (ex: tentando adivinhar senhas, ou floodando mensagens)
// sobrecarregue o servidor. É importante e complementar, mas não substitui o
// Cloudflare.
//
// Técnica usada: "token bucket" (balde de fichas). Cada IP ganha um balde que
// enche a uma taxa constante. Cada pedido gasta uma ficha. Sem fichas = pedido
// recusado com 429 (Too Many Requests).
// ---------------------------------------------------------------------------
package ratelimit

import (
	"net"
	"net/http"
	"sync"
	"time"

	"golang.org/x/time/rate"
)

// visitor guarda o limitador de um IP e quando ele foi visto por último
// (pra podermos limpar IPs inativos da memória).
type visitor struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

type Limiter struct {
	mu       sync.Mutex
	visitors map[string]*visitor
	rate     rate.Limit // fichas por segundo
	burst    int        // tamanho do balde (pico permitido)
}

// New cria um limitador. Ex: New(5, 10) = repõe 5 pedidos/seg, tolera picos de 10.
func New(perSecond float64, burst int) *Limiter {
	l := &Limiter{
		visitors: make(map[string]*visitor),
		rate:     rate.Limit(perSecond),
		burst:    burst,
	}
	// Uma rotina em segundo plano limpa IPs que sumiram há mais de 10 min,
	// pra memória não crescer pra sempre.
	go l.cleanupLoop()
	return l
}

func (l *Limiter) getVisitor(ip string) *rate.Limiter {
	l.mu.Lock()
	defer l.mu.Unlock()

	v, exists := l.visitors[ip]
	if !exists {
		lim := rate.NewLimiter(l.rate, l.burst)
		l.visitors[ip] = &visitor{limiter: lim, lastSeen: time.Now()}
		return lim
	}
	v.lastSeen = time.Now()
	return v.limiter
}

func (l *Limiter) cleanupLoop() {
	for {
		time.Sleep(time.Minute)
		l.mu.Lock()
		for ip, v := range l.visitors {
			if time.Since(v.lastSeen) > 10*time.Minute {
				delete(l.visitors, ip)
			}
		}
		l.mu.Unlock()
	}
}

// Middleware embrulha um handler aplicando o limite por IP.
func (l *Limiter) Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ip := clientIP(r)
		if !l.getVisitor(ip).Allow() {
			http.Error(w, "muitos pedidos, tente de novo em instantes", http.StatusTooManyRequests)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// clientIP tenta descobrir o IP real do cliente. Quando estamos atrás do
// Cloudflare, o IP verdadeiro vem no cabeçalho "CF-Connecting-IP".
func clientIP(r *http.Request) string {
	if cf := r.Header.Get("CF-Connecting-IP"); cf != "" {
		return cf
	}
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		// X-Forwarded-For pode ter vários IPs; o primeiro é o cliente original.
		if i := indexByte(xff, ','); i >= 0 {
			return trim(xff[:i])
		}
		return trim(xff)
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

// pequenos helpers pra não depender do pacote strings aqui
func indexByte(s string, b byte) int {
	for i := 0; i < len(s); i++ {
		if s[i] == b {
			return i
		}
	}
	return -1
}
func trim(s string) string {
	start, end := 0, len(s)
	for start < end && s[start] == ' ' {
		start++
	}
	for end > start && s[end-1] == ' ' {
		end--
	}
	return s[start:end]
}
