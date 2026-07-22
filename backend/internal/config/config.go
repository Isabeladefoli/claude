// Package config carrega as configurações do servidor a partir de variáveis
// de ambiente. A ideia: NADA de segredo fica escrito no código (hardcoded).
// Tudo sensível (chave JWT, caminho do banco) vem do ambiente. Isso é uma
// boa prática de segurança — se o código vazar, os segredos não vão junto.
package config

import (
	"crypto/rand"
	"encoding/hex"
	"log"
	"os"
)

type Config struct {
	// Endereço onde o servidor escuta. Ex: ":8080" = todas as interfaces na porta 8080.
	Addr string

	// Caminho do arquivo SQLite. Ex: "data/messenger.db".
	DBPath string

	// Pasta onde os arquivos de mídia (fotos, áudios, vídeos, foto de perfil)
	// ficam guardados no disco. O banco só guarda um "vale" apontando pra cá.
	MediaDir string

	// Segredo usado para assinar os tokens JWT de sessão. Se alguém descobrir
	// isso, consegue forjar login de qualquer usuário — então é CRÍTICO que
	// seja aleatório e secreto em produção.
	JWTSecret []byte
}

// getEnv retorna a variável de ambiente ou um valor padrão se ela não existir.
func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// Load monta a configuração lendo o ambiente.
func Load() *Config {
	cfg := &Config{
		Addr:     getEnv("MSG_ADDR", ":8080"),
		DBPath:   getEnv("MSG_DB_PATH", "data/messenger.db"),
		MediaDir: getEnv("MSG_MEDIA_DIR", "data/media"),
	}

	// A chave JWT: se você definir MSG_JWT_SECRET no ambiente, usamos ela.
	// Se NÃO definir, geramos uma aleatória na hora. Cuidado: uma chave gerada
	// na hora muda toda vez que o servidor reinicia — o que desloga todo mundo.
	// Bom para testar, ruim para produção. Em produção, SEMPRE defina a env.
	secret := os.Getenv("MSG_JWT_SECRET")
	if secret == "" {
		buf := make([]byte, 32) // 256 bits de aleatoriedade
		if _, err := rand.Read(buf); err != nil {
			log.Fatalf("não consegui gerar segredo JWT: %v", err)
		}
		secret = hex.EncodeToString(buf)
		log.Println("⚠️  MSG_JWT_SECRET não definido — gerando um temporário. " +
			"Todos serão deslogados ao reiniciar. Defina a env em produção!")
	}
	cfg.JWTSecret = []byte(secret)

	return cfg
}
