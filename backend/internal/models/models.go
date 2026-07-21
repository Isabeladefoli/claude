// Package models define as estruturas de dados centrais do app: usuários,
// mensagens e grupos. Pense nelas como o "formato" de cada linha do banco.
package models

import "time"

// User representa uma conta. Repare em detalhes importantes de segurança:
//   - Guardamos PasswordHash, NUNCA a senha em texto puro.
//   - Email e Phone são opcionais (ponteiros que podem ser nil) — o usuário
//     escolhe se quer cadastrar.
//   - PublicKey é a chave pública de criptografia do usuário. A chave PRIVADA
//     correspondente NUNCA chega ao servidor: ela fica só no celular. É isso
//     que torna a criptografia ponta-a-ponta (E2E) real.
type User struct {
	ID           int64     `json:"id"`
	Username     string    `json:"username"`
	PasswordHash string    `json:"-"` // `json:"-"` = nunca sai numa resposta JSON
	Email        *string   `json:"email,omitempty"`
	Phone        *string   `json:"phone,omitempty"`
	PublicKey    string    `json:"public_key"` // chave pública X25519, base64
	AvatarURL    *string   `json:"avatar_url,omitempty"`
	CreatedAt    time.Time `json:"created_at"`
}

// Message representa uma mensagem trocada entre dois usuários (ou num grupo).
//
// Detalhe crucial: o campo Ciphertext guarda o texto JÁ CRIPTOGRAFADO pelo
// celular do remetente. O servidor só recebe, guarda e repassa esse "blob"
// embaralhado — ele NÃO consegue ler o conteúdo. Por isso os campos são
// Ciphertext e Nonce, não "Text". Quem descriptografa é o celular do
// destinatário, usando a chave privada dele.
type Message struct {
	ID          int64     `json:"id"`
	SenderID    int64     `json:"sender_id"`
	RecipientID *int64    `json:"recipient_id,omitempty"` // nil se for mensagem de grupo
	GroupID     *int64    `json:"group_id,omitempty"`     // nil se for 1-para-1
	Ciphertext  string    `json:"ciphertext"`             // conteúdo criptografado, base64
	Nonce       string    `json:"nonce"`                  // "número usado uma vez" da criptografia, base64
	CreatedAt   time.Time `json:"created_at"`
}

// Group representa uma conversa em grupo.
type Group struct {
	ID        int64     `json:"id"`
	Name      string    `json:"name"`
	OwnerID   int64     `json:"owner_id"`
	CreatedAt time.Time `json:"created_at"`
}
