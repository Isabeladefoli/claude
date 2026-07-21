# 🔒 App de Mensagens Privadas

Um app de mensagens focado em **privacidade de verdade**: criptografia
ponta-a-ponta, servidor que roda no **seu** PC (nada de big tech guardando
seus dados), Android + iOS com Kotlin Multiplatform.

> Projeto de portfólio — feito pra aprender a lógica por trás, não só copiar código.
> A explicação completa da arquitetura está em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

## Status do projeto

- [x] **Fase 1 — Backend (Go):** auth, mensagens 1-a-1, **grupos**, lista de conversas, tempo real, anti-abuso, **testes** ✅
- [x] **Fase 2a — App Android (Kotlin + Compose):** cadastro, login, conversas e chat em tempo real (fluxo, sem design) ✅ → veja [`android/README.md`](android/README.md)
- [ ] Fase 2b — Criptografia E2E de verdade (libsodium) no app
- [ ] Fase 2c — Migrar a camada de dados pra Kotlin Multiplatform + iOS
- [ ] Fase 3 — Design da interface
- [ ] Fase 4 — Fotos (feed e perfil)

---

## Rodando o servidor (no seu PC Ubuntu)

Precisa do [Go 1.24+](https://go.dev/dl/) instalado.

```bash
cd backend

# baixa as dependências
go mod tidy

# roda o servidor
go run ./cmd/server

# roda os testes automatizados
go test ./...
```

O servidor sobe em `http://localhost:8080`. Testa se está no ar:

```bash
curl localhost:8080/health   # deve responder: ok
```

### Configuração (variáveis de ambiente)

| Variável | Padrão | O que é |
|---|---|---|
| `MSG_ADDR` | `:8080` | Endereço/porta onde escuta |
| `MSG_DB_PATH` | `data/messenger.db` | Caminho do arquivo do banco |
| `MSG_JWT_SECRET` | *(gerado na hora)* | Segredo pra assinar tokens. **Defina em produção!** |

Em produção, gere um segredo forte e fixe ele:

```bash
export MSG_JWT_SECRET=$(openssl rand -hex 32)
go run ./cmd/server
```

> ⚠️ Se você **não** definir `MSG_JWT_SECRET`, um segredo aleatório é gerado a
> cada reinício — e todo mundo é deslogado quando o servidor reinicia. Ok pra
> testar, ruim pra valer.

---

## Testando a API na mão

```bash
# 1) cria uma conta (a public_key aqui é só um exemplo; o app real gera a de verdade)
curl -X POST localhost:8080/api/register \
  -d '{"username":"ana","password":"senha12345","public_key":"exemplo"}'

# guarda o "token" que voltou e usa nas próximas chamadas:
TOKEN="cole-o-token-aqui"

# 2) vê seu perfil
curl localhost:8080/api/me -H "Authorization: Bearer $TOKEN"

# 3) manda mensagem pro usuário de id 2 (o ciphertext real vem do app)
curl -X POST localhost:8080/api/messages -H "Authorization: Bearer $TOKEN" \
  -d '{"recipient_id":2,"ciphertext":"embaralhado","nonce":"xyz"}'
```

---

## Expondo pra internet com Cloudflare Tunnel

Isso deixa seus amigos acessarem seu servidor caseiro **sem** abrir portas no
roteador e **com** proteção DDoS do Cloudflare na frente.

1. Instale o `cloudflared` no seu Ubuntu:
   ```bash
   # veja o guia oficial pra baixar o pacote da sua distro:
   # https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/
   ```
2. Faça login (abre o navegador pra conectar na sua conta Cloudflare):
   ```bash
   cloudflared tunnel login
   ```
3. Crie o túnel e aponte pro servidor local:
   ```bash
   cloudflared tunnel create messenger
   cloudflared tunnel route dns messenger chat.seudominio.com
   cloudflared tunnel --url http://localhost:8080 run messenger
   ```

Pronto: `https://chat.seudominio.com` chega no seu PC pelo túnel seguro. É essa
URL que os apps vão usar.

> Pra só testar rápido (URL temporária, sem domínio):
> ```bash
> cloudflared tunnel --url http://localhost:8080
> ```

---

## Segurança — resumo

- **Senhas:** hash Argon2id com salt (nunca em texto puro).
- **Sessões:** tokens JWT assinados, com validade.
- **Conteúdo:** o servidor só transporta texto **já criptografado** (E2E — a
  criptografia acontece no app, na Fase 2).
- **Anti-abuso:** rate limit por IP + timeouts + limite de tamanho de corpo.
- **DDoS:** filtragem na camada Cloudflare (a defesa realista pra servidor caseiro).

Detalhes e o "porquê" de cada escolha em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).
