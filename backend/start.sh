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

# Notificações push: procura a chave de conta de serviço do Firebase na pasta
# do backend (service-account.json ou o nome que o Firebase baixa). Se achar,
# liga o envio real de push; se não achar, o servidor sobe em modo "mock"
# (mensagens funcionam, só não chegam notificações com o app fechado).
FCM_KEY=$(ls service-account.json *firebase-adminsdk*.json 2>/dev/null | head -1)
if [ -n "$FCM_KEY" ]; then
  export MSG_FCM_CREDENTIALS="$(pwd)/$FCM_KEY"
  echo "Firebase encontrado: $FCM_KEY (notificações push ligadas)"
else
  echo "AVISO: nenhuma chave do Firebase encontrada — notificações em modo mock."
fi

go run ./cmd/server
