// Package db cuida da conexão com o SQLite e da criação das tabelas.
//
// Por que SQLite? É um banco que roda dentro de um único arquivo, sem
// servidor separado. Perfeito para começar no seu PC Ubuntu: zero configuração.
// Quando o app crescer, dá pra migrar para PostgreSQL sem mudar a lógica.
//
// Usamos o driver "modernc.org/sqlite" que é escrito em Go puro — ou seja,
// não precisa de bibliotecas C do sistema, compila e roda em qualquer lugar.
package db

import (
	"database/sql"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	_ "modernc.org/sqlite" // driver SQLite (o "_" registra ele sem uso direto)
)

// Open abre (ou cria) o banco no caminho informado e roda as migrations.
func Open(path string) (*sql.DB, error) {
	// Garante que a pasta onde o arquivo .db vai morar existe.
	if dir := filepath.Dir(path); dir != "" && dir != "." {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return nil, fmt.Errorf("criando pasta do banco: %w", err)
		}
	}

	// "_pragma=foreign_keys(1)" liga a checagem de chaves estrangeiras —
	// impede, por exemplo, uma mensagem apontar para um usuário que não existe.
	// "_pragma=journal_mode(WAL)" melhora a performance com leituras/escritas
	// simultâneas (importante num chat em tempo real).
	dsn := path + "?_pragma=foreign_keys(1)&_pragma=journal_mode(WAL)&_pragma=busy_timeout(5000)"

	database, err := sql.Open("sqlite", dsn)
	if err != nil {
		return nil, fmt.Errorf("abrindo banco: %w", err)
	}

	// Confirma que a conexão realmente funciona.
	if err := database.Ping(); err != nil {
		return nil, fmt.Errorf("testando conexão: %w", err)
	}

	if err := migrate(database); err != nil {
		return nil, fmt.Errorf("rodando migrations: %w", err)
	}

	return database, nil
}

// migrate cria as tabelas se elas ainda não existirem. Rodar isso toda vez
// que o servidor sobe é seguro por causa do "IF NOT EXISTS".
func migrate(db *sql.DB) error {
	schema := `
	CREATE TABLE IF NOT EXISTS users (
		id            INTEGER PRIMARY KEY AUTOINCREMENT,
		username      TEXT    NOT NULL UNIQUE COLLATE NOCASE, -- NOCASE: "Ana" == "ana"
		password_hash TEXT    NOT NULL,
		name          TEXT,   -- nome de exibição (opcional)
		birthday      TEXT,   -- data de nascimento em texto (opcional)
		email         TEXT,
		phone         TEXT,
		public_key    TEXT    NOT NULL,
		avatar_url    TEXT,
		created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE IF NOT EXISTS groups (
		id         INTEGER PRIMARY KEY AUTOINCREMENT,
		name       TEXT    NOT NULL,
		owner_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Quem pertence a qual grupo. Uma linha por (usuário, grupo).
	CREATE TABLE IF NOT EXISTS group_members (
		group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
		user_id  INTEGER NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
		PRIMARY KEY (group_id, user_id)
	);

	CREATE TABLE IF NOT EXISTS messages (
		id           INTEGER PRIMARY KEY AUTOINCREMENT,
		sender_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		recipient_id INTEGER REFERENCES users(id)  ON DELETE CASCADE, -- NULL em grupo
		group_id     INTEGER REFERENCES groups(id) ON DELETE CASCADE, -- NULL em 1-a-1
		ciphertext   TEXT    NOT NULL, -- conteúdo já criptografado
		nonce        TEXT    NOT NULL,
		created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Índices deixam as buscas de histórico rápidas.
	CREATE INDEX IF NOT EXISTS idx_messages_dm    ON messages(sender_id, recipient_id, created_at);
	CREATE INDEX IF NOT EXISTS idx_messages_group ON messages(group_id, created_at);

	-- Contatos: a relação de UM usuário (owner) com OUTRO (contact).
	--   saved  = 1  -> foi salvo/adicionado de propósito (aparece em "Salvos")
	--   hidden = 1  -> foi apagado da lista "Todos" (pra deixar limpo), mas
	--                  continua existindo (ainda achável na busca e em "Salvos")
	-- Uma linha por par (owner, contact).
	CREATE TABLE IF NOT EXISTS contacts (
		owner_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		contact_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		saved      INTEGER NOT NULL DEFAULT 0,
		hidden     INTEGER NOT NULL DEFAULT 0,
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
		PRIMARY KEY (owner_id, contact_id)
	);

	-- Mídia (foto, áudio, vídeo, foto de perfil). O ARQUIVO em si mora no disco
	-- (pasta de mídia); aqui guardamos só os dados dele. O "id" é aleatório e
	-- também é o nome do arquivo no disco.
	CREATE TABLE IF NOT EXISTS media (
		id         TEXT    PRIMARY KEY,
		owner_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		mime       TEXT    NOT NULL, -- tipo do arquivo (ex: image/jpeg)
		size       INTEGER NOT NULL, -- tamanho em bytes
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Controle de "lidas": até qual mensagem (id) o dono já leu de cada conversa.
	-- Serve pra contar quantas mensagens não-lidas cada chat tem.
	CREATE TABLE IF NOT EXISTS chat_reads (
		owner_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		other_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		last_read_id INTEGER NOT NULL DEFAULT 0,
		PRIMARY KEY (owner_id, other_id)
	);

	-- Bloqueios: blocker bloqueou blocked (uma linha por par).
	CREATE TABLE IF NOT EXISTS blocks (
		blocker_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		blocked_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
		PRIMARY KEY (blocker_id, blocked_id)
	);

	-- Denúncias de usuário (vão pra dona do app ler/encaminhar por e-mail depois).
	CREATE TABLE IF NOT EXISTS reports (
		id          INTEGER PRIMARY KEY AUTOINCREMENT,
		reporter_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
		reported_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
		reason      TEXT    NOT NULL,
		created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Reports de suporte que o usuário manda pela tela "Support". Guardamos aqui
	-- pra que a dona do app leia; o e-mail de resposta é o que a pessoa digitou.
	CREATE TABLE IF NOT EXISTS support_reports (
		id          INTEGER PRIMARY KEY AUTOINCREMENT,
		user_id     INTEGER REFERENCES users(id) ON DELETE SET NULL,
		reply_email TEXT    NOT NULL,
		title       TEXT    NOT NULL,
		body        TEXT    NOT NULL,
		created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	-- Device tokens pra notificações push (FCM). Um usuário pode ter múltiplos
	-- aparelhos (multi-device), cada um com seu token.
	CREATE TABLE IF NOT EXISTS device_tokens (
		id         INTEGER PRIMARY KEY AUTOINCREMENT,
		user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
		token      TEXT    NOT NULL UNIQUE, -- token do FCM
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
	);

	CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens(user_id);
	`

	if _, err := db.Exec(schema); err != nil {
		return err
	}

	// Migrações "adiciona coluna" para bancos que já existiam ANTES desses campos.
	// Em banco novo as colunas já vêm do CREATE acima; aqui garantimos os antigos.
	// SQLite não tem "ADD COLUMN IF NOT EXISTS", então ignoramos o erro de coluna
	// duplicada (que só quer dizer "já existe, tudo certo").
	addColumns := []string{
		`ALTER TABLE users ADD COLUMN name TEXT`,
		`ALTER TABLE users ADD COLUMN birthday TEXT`,
	}
	for _, stmt := range addColumns {
		if _, err := db.Exec(stmt); err != nil && !strings.Contains(err.Error(), "duplicate column name") {
			return fmt.Errorf("migração %q: %w", stmt, err)
		}
	}
	return nil
}
