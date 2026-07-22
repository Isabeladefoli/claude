#!/bin/bash
# Liga o servidor. Roda: ./start.sh
cd "$(dirname "$0")"
go run ./cmd/server
