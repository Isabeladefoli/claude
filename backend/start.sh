#!/bin/bash
# Liga o servidor. Roda: ./start.sh
cd "$(dirname "$0")"

# Reaproveita um segredo de JWT fixo entre reinícios (gera na primeira vez e
# salva em .jwt_secret, que fica de fora do git). Sem isso, cada reinício
# gerava um segredo novo e deslogava todo mundo.
SECRET_FILE=".jwt_secret"
if [ ! -f "$SECRET_FILE" ]; then
  openssl rand -hex 32 > "$SECRET_FILE"
fi
export MSG_JWT_SECRET=$(cat "$SECRET_FILE")

go run ./cmd/server
