package auth

import "testing"

// Testes do módulo mais crítico de segurança: hashing de senha.

func TestHashAndVerify(t *testing.T) {
	senha := "minha-senha-super-secreta"

	hash, err := HashPassword(senha)
	if err != nil {
		t.Fatalf("hash falhou: %v", err)
	}

	// O hash NUNCA pode ser igual à senha (senão não seria hash).
	if hash == senha {
		t.Fatal("o hash não pode ser a própria senha")
	}

	// A senha certa deve verificar como verdadeira.
	ok, err := VerifyPassword(senha, hash)
	if err != nil {
		t.Fatalf("verificação falhou: %v", err)
	}
	if !ok {
		t.Error("senha correta deveria verificar como válida")
	}

	// A senha errada deve verificar como falsa.
	ok, _ = VerifyPassword("senha-errada", hash)
	if ok {
		t.Error("senha errada não deveria verificar como válida")
	}
}

func TestSaltMakesHashesUnique(t *testing.T) {
	// Duas pessoas com a MESMA senha devem ter hashes DIFERENTES, graças ao
	// salt aleatório. Isso derruba ataques com tabelas pré-computadas.
	h1, _ := HashPassword("mesma-senha")
	h2, _ := HashPassword("mesma-senha")
	if h1 == h2 {
		t.Error("hashes da mesma senha deveriam diferir por causa do salt")
	}
}

func TestVerifyRejectsGarbage(t *testing.T) {
	// Um hash malformado deve dar erro, não passar.
	if _, err := VerifyPassword("qualquer", "isso-nao-e-um-hash"); err == nil {
		t.Error("esperava erro ao verificar contra um hash inválido")
	}
}
