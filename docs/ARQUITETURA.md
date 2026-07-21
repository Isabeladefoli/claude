# Arquitetura do App de Mensagens Privadas

Este documento explica **a lógica por trás** de cada decisão. A ideia é você
entender o "porquê", não só copiar código.

---

## Visão geral

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│  App Android │        │              │        │   App iOS    │
│   (Kotlin)   │◄──────►│  Cloudflare  │◄──────►│   (Kotlin    │
│              │  HTTPS  │    Tunnel    │  HTTPS  │  Multiplatf) │
└──────────────┘        └──────┬───────┘        └──────────────┘
                               │
                               │ (túnel seguro, sem abrir porta no seu roteador)
                               ▼
                       ┌───────────────┐
                       │  Servidor Go  │   ← roda no seu PC Ubuntu
                       │  + SQLite     │
                       └───────────────┘
```

- **Apps (Kotlin Multiplatform):** a interface. Rodam no celular. É onde a
  criptografia acontece.
- **Cloudflare Tunnel:** a "ponte" segura entre a internet e o seu PC. Também
  é sua principal defesa contra DDoS.
- **Servidor Go:** recebe pedidos, guarda dados, entrega mensagens em tempo real.
- **SQLite:** o banco de dados (um arquivo só, simples de começar).

---

## As 3 camadas de segurança (e o que cada uma protege)

### 1. Transporte: HTTPS + Cloudflare
Tudo entre o celular e o servidor viaja criptografado (TLS/HTTPS). O Cloudflare
fica na frente e filtra tráfego malicioso **antes** de chegar no seu PC. É isso
que de fato te protege de um DDoS grande — não dá pra bloquear um DDoS de
verdade dentro do seu próprio servidor, porque quando o tráfego chega até ele o
estrago já aconteceu.

### 2. Autenticação: senhas com Argon2id + tokens JWT
- Senhas **nunca** são guardadas em texto. Guardamos um "hash" Argon2id
  (mão única, lento de propósito, com salt único). Se o banco vazar, as senhas
  continuam protegidas.
- Depois do login, o servidor entrega um **token JWT** assinado. O celular
  apresenta esse token em cada pedido, sem reenviar a senha.
- Detalhe: login com senha errada e usuário inexistente dão a **mesma** resposta,
  pra não revelar quais usuários existem.

### 3. Conteúdo: criptografia ponta-a-ponta (E2E)
Esta é a parte mais importante do "privacidade de verdade":

> O **servidor nunca vê o conteúdo das mensagens.**

Como isso funciona:

1. Cada usuário tem um **par de chaves**: uma **pública** e uma **privada**.
   - A pública pode ser compartilhada com todo mundo.
   - A privada **nunca sai do celular**.
2. No registro, o celular gera esse par e envia **só a chave pública** ao servidor.
3. Pra Ana mandar "oi" pro Beto:
   - O celular da Ana pega a chave **pública do Beto** e criptografa "oi".
   - Vira um "ciphertext" embaralhado. O servidor só recebe e guarda isso.
4. No celular do Beto, o ciphertext é decifrado com a chave **privada dele**.

Nem o servidor, nem o Cloudflare, nem sua operadora conseguem ler. É o mesmo
princípio que o Signal e o WhatsApp usam — a diferença é que aqui **você**
controla o servidor.

> **Nota honesta sobre o estado atual:** o backend já está pronto pra E2E — ele
> só transporta ciphertext, nunca texto puro. A geração de chaves e a
> criptografia em si acontecem no **app** (próxima fase, com a lib libsodium).
> Uma evolução futura é o "Double Ratchet" (o que dá ao Signal o *forward
> secrecy*: mesmo que uma chave vaze, mensagens antigas continuam seguras).

#### E2E em grupo é diferente (e mais difícil)

No 1-a-1, a Ana criptografa com a chave pública do Beto e pronto. Num grupo de
10 pessoas, criptografar 10 vezes a cada mensagem seria caro. A solução (mesma
do WhatsApp/Signal) chama-se **"sender key"** (chave de remetente):

1. Cada membro cria uma chave secreta própria e a envia aos outros membros
   **usando o canal 1-a-1 já criptografado** (reaproveita o mecanismo de cima).
2. Depois, criptografa cada mensagem **uma vez** com essa chave, e todos abrem.

O **servidor continua burro**: ele só guarda quem está em qual grupo e distribui
(*fan-out*) o envelope fechado pra todos os membros. Ele nunca vê chave nem
texto. A distribuição das sender keys é toda no app (Fase 2).

---

## Por que essas escolhas de tecnologia?

| Escolha | Por quê |
|---|---|
| **Go** no servidor | Rápido, ótimo pra muitas conexões simultâneas (chat), deploy é um binário só. |
| **SQLite** | Zero configuração pra começar. Um arquivo. Migra pra PostgreSQL depois sem mudar a lógica. |
| **WebSocket** | Conexão que fica aberta, deixa o servidor "empurrar" mensagens na hora (tempo real). |
| **JWT** | Sessão sem precisar guardar estado no servidor; escala fácil. |
| **Argon2id** | Algoritmo de hash de senha mais recomendado hoje. |
| **Kotlin Multiplatform** | Um código de lógica compartilhado entre Android e iOS. |
| **Cloudflare Tunnel** | Expõe seu PC com segurança, sem abrir portas no roteador, com proteção DDoS. |

---

## Como uma mensagem viaja (passo a passo)

```
Ana digita "oi"
   │
   ▼
[celular da Ana] criptografa com a chave pública do Beto → ciphertext + nonce
   │
   ▼  POST /api/messages  (só ciphertext, via HTTPS)
[Cloudflare] filtra/protege
   │
   ▼
[Servidor Go] salva no SQLite + procura o Beto no Hub de WebSocket
   │
   ├─ Beto ONLINE  → empurra na hora pelo WebSocket
   └─ Beto OFFLINE → fica salvo; ele pega no histórico ao abrir o app
   │
   ▼
[celular do Beto] decifra com a chave PRIVADA dele → "oi"
```

---

## Estrutura de pastas do backend

```
backend/
├── cmd/server/main.go        ← ponto de entrada; monta tudo e define as rotas
├── internal/
│   ├── config/               ← lê configuração do ambiente (segredos, portas)
│   ├── db/                   ← conexão SQLite + criação das tabelas
│   ├── models/               ← formato dos dados (User, Message, Group)
│   ├── auth/                 ← senhas (Argon2), tokens (JWT), "porteiro" (middleware)
│   ├── handlers/             ← lógica de cada rota (registrar, logar, mensagens)
│   ├── ws/                   ← WebSocket: conexões em tempo real
│   └── ratelimit/            ← limita pedidos por IP (anti-abuso)
```

---

## Rotas da API (referência rápida)

| Método | Rota | Precisa login? | O que faz |
|---|---|---|---|
| GET | `/health` | não | Diz se o servidor está no ar |
| POST | `/api/register` | não | Cria conta (username, senha, chave pública) |
| POST | `/api/login` | não | Loga e devolve um token |
| GET | `/api/me` | sim | Dados do usuário logado |
| GET | `/api/users/{username}` | sim | Perfil + chave pública de alguém |
| POST | `/api/messages` | sim | Envia mensagem 1-a-1 (já criptografada) |
| GET | `/api/conversations` | sim | Lista de conversas (tela inicial) |
| GET | `/api/conversations/{outroID}` | sim | Histórico de uma conversa 1-a-1 |
| POST | `/api/groups` | sim | Cria um grupo |
| GET | `/api/groups` | sim | Lista meus grupos |
| GET | `/api/groups/{id}` | sim | Detalhes + membros (só membros) |
| POST | `/api/groups/{id}/members` | sim | Adiciona membro (só o dono) |
| DELETE | `/api/groups/{id}/members/{userID}` | sim | Remove/sai do grupo |
| POST | `/api/groups/{id}/messages` | sim | Envia mensagem no grupo |
| GET | `/api/groups/{id}/messages` | sim | Histórico do grupo (só membros) |
| WS | `/ws?token=...` | sim | Canal de tempo real |

---

## Próximas fases

- **Fase 2 — Apps (KMP):** gerar chaves no celular, criptografar/descriptografar
  com libsodium, telas de login e chat (Jetpack Compose no Android, SwiftUI no iOS).
- **Fase 3 — Design:** refinar a interface, animações.
- **Fase 4 — Fotos:** upload de fotos, feed e foto de perfil.
- **Grupos:** as tabelas já existem no banco; falta a API e a UI.
