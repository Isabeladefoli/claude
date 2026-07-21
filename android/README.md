# App Android — Private Messenger

App Android nativo em **Kotlin + Jetpack Compose**, sem design ainda (só o
fluxo funcionando): cadastro, login, lista de conversas e chat com mensagens em
tempo real.

> Camada de rede escrita com **Ktor** (cliente multiplataforma) de propósito:
> quando migrarmos pra Kotlin Multiplatform (Android + iOS), quase tudo em
> `data/` é reaproveitado.

## Como abrir e rodar

1. Abra a pasta `android/` no **Android Studio** (ele instala o Android SDK
   automaticamente na primeira vez).
2. Suba o servidor Go (veja o README da raiz): `cd backend && go run ./cmd/server`.
3. Rode o app num **emulador** (botão ▶️ Run).
   - O app já vem apontando pra `http://10.0.2.2:8080`, que é como o emulador
     enxerga o `localhost` do seu PC. Não precisa configurar nada.
   - Em **celular físico**, troque a URL base pela do seu Cloudflare Tunnel
     (por enquanto, isso é definido em `TokenStore` → `base_url`).

## Como testar

1. Abra o app → **Criar conta** (ex: usuário `ana`, senha com 8+ caracteres).
2. No emulador, abra uma segunda instância (ou crie outra conta `beto`).
3. Em `ana`, digite `beto` no campo "Usuário para conversar" → **Abrir**.
4. Mande mensagens. Se os dois estiverem abertos, elas chegam em **tempo real**.

## Estrutura

```
app/src/main/java/com/privatemessenger/app/
├── MainActivity.kt          ← entrada; decide login vs. área logada; navegação
├── data/                    ← camada de dados (reaproveitável no KMP futuro)
│   ├── Dtos.kt              ← classes que espelham o JSON do servidor
│   ├── ApiClient.kt        ← chamadas HTTP (Ktor)
│   ├── RealtimeClient.kt   ← WebSocket (mensagens em tempo real)
│   ├── TokenStore.kt       ← guarda token/sessão no aparelho (DataStore)
│   ├── DeviceKeys.kt       ← chaves de criptografia (placeholder por enquanto)
│   └── MessengerRepository.kt ← junta tudo; a interface fala só com ela
├── vm/                      ← ViewModels (estado + lógica de cada tela)
│   ├── AuthViewModel.kt
│   ├── ConversationsViewModel.kt
│   └── ChatViewModel.kt
└── ui/                      ← telas em Jetpack Compose (sem design ainda)
    ├── AuthScreen.kt
    ├── ConversationsScreen.kt
    ├── ChatScreen.kt
    └── ViewModelFactories.kt
```

## O que ainda falta (próximos passos)

- **Criptografia E2E de verdade:** hoje o texto vai puro no campo `ciphertext`.
  Procure os comentários `TODO E2E` em `MessengerRepository.kt` e `DeviceKeys.kt`
  — é ali que a criptografia com libsodium entra, sem mudar o resto do app.
- **Design:** cores, tema, layout bonito das mensagens (bolhas), avatares.
- **Grupos, fotos e voz:** o backend já suporta grupos; falta a interface.
