// Package auth cuida de dois assuntos de segurança: guardar senhas com
// segurança (este arquivo) e criar/verificar sessões de login (token.go).
package auth

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/base64"
	"errors"
	"fmt"
	"strings"

	"golang.org/x/crypto/argon2"
)

// ---------------------------------------------------------------------------
// A LÓGICA por trás de guardar senhas:
//
// NUNCA guardamos a senha do usuário como ela é. Se o banco vazar, o atacante
// não pode ter as senhas. Então guardamos um "hash": um embaralhamento de mão
// única. Da senha dá pra gerar o hash, mas do hash NÃO dá pra voltar à senha.
//
// Usamos Argon2id, que é o algoritmo vencedor da competição de hashing de
// senhas e o recomendado hoje. Ele é DE PROPÓSITO lento e consome memória,
// para dificultar ataques de força bruta (testar milhões de senhas por segundo).
//
// Também usamos um "salt": um valor aleatório único por senha. Isso impede que
// duas pessoas com a mesma senha tenham o mesmo hash, e derruba ataques com
// tabelas pré-computadas (rainbow tables).
// ---------------------------------------------------------------------------

// Parâmetros do Argon2id. Valores equilibrados para um PC caseiro.
const (
	argonTime    = 1         // número de iterações
	argonMemory  = 64 * 1024 // memória usada, em KiB (= 64 MB)
	argonThreads = 4         // paralelismo
	argonKeyLen  = 32        // tamanho do hash final, em bytes
	saltLen      = 16        // tamanho do salt, em bytes
)

// HashPassword recebe a senha em texto puro e devolve uma string pronta pra
// guardar no banco. O formato guarda o salt e os parâmetros junto do hash,
// assim conseguimos verificar depois mesmo se mudarmos os parâmetros.
func HashPassword(password string) (string, error) {
	salt := make([]byte, saltLen)
	if _, err := rand.Read(salt); err != nil {
		return "", fmt.Errorf("gerando salt: %w", err)
	}

	hash := argon2.IDKey([]byte(password), salt, argonTime, argonMemory, argonThreads, argonKeyLen)

	// Formato padrão PHC: $argon2id$v=19$m=...,t=...,p=...$salt$hash
	b64 := base64.RawStdEncoding.EncodeToString
	encoded := fmt.Sprintf("$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s",
		argon2.Version, argonMemory, argonTime, argonThreads,
		b64(salt), b64(hash))
	return encoded, nil
}

var ErrInvalidHash = errors.New("formato de hash inválido")

// VerifyPassword confere se a senha digitada bate com o hash guardado.
// Retorna true se bater. Recalcula o hash da senha digitada usando o MESMO
// salt e parâmetros guardados e compara os resultados.
func VerifyPassword(password, encoded string) (bool, error) {
	parts := strings.Split(encoded, "$")
	// Esperamos: ["", "argon2id", "v=19", "m=...,t=...,p=...", salt, hash]
	if len(parts) != 6 || parts[1] != "argon2id" {
		return false, ErrInvalidHash
	}

	var memory, time uint32
	var threads uint8
	if _, err := fmt.Sscanf(parts[3], "m=%d,t=%d,p=%d", &memory, &time, &threads); err != nil {
		return false, ErrInvalidHash
	}

	salt, err := base64.RawStdEncoding.DecodeString(parts[4])
	if err != nil {
		return false, ErrInvalidHash
	}
	wantHash, err := base64.RawStdEncoding.DecodeString(parts[5])
	if err != nil {
		return false, ErrInvalidHash
	}

	gotHash := argon2.IDKey([]byte(password), salt, time, memory, threads, uint32(len(wantHash)))

	// subtle.ConstantTimeCompare compara sem "vazar" informação pelo tempo que
	// leva. Uma comparação normal (==) pode revelar quantos bytes bateram pela
	// velocidade — isso é um ataque real (timing attack). Aqui evitamos isso.
	if subtle.ConstantTimeCompare(gotHash, wantHash) == 1 {
		return true, nil
	}
	return false, nil
}
