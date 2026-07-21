package com.privatemessenger.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// MessengerRepository é a "porta de entrada" da camada de dados. As telas e os
// ViewModels falam SÓ com ela, sem saber se por baixo é HTTP, WebSocket, cache,
// etc. Isso mantém a interface separada dos detalhes de rede (boa arquitetura).
// ---------------------------------------------------------------------------
class MessengerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(appContext)
    private val api = ApiClient(tokenStore)

    // Um escopo de corrotinas próprio pra tarefas de fundo (ex: o WebSocket).
    private val scope = CoroutineScope(SupervisorJob())
    private val realtime = RealtimeClient(api, tokenStore, scope)

    // Fluxo de mensagens que chegam em tempo real (o WebSocket alimenta isto).
    val incomingMessages: Flow<Message> = realtime.incoming

    // Fluxos de sessão que a interface observa pra decidir se mostra login ou
    // a lista de conversas.
    val token: Flow<String?> = tokenStore.token
    val userId: Flow<Long?> = tokenStore.userId
    val baseUrl: Flow<String> = tokenStore.baseUrl

    // --- Autenticação ---

    suspend fun register(username: String, password: String, publicKey: String): AuthResponse {
        val auth = api.register(RegisterRequest(username, password, publicKey))
        realtime.start() // já conecta o tempo real após entrar
        return auth
    }

    suspend fun login(username: String, password: String): AuthResponse {
        val auth = api.login(LoginRequest(username, password))
        realtime.start()
        return auth
    }

    suspend fun logout() {
        realtime.stop()
        tokenStore.clear()
    }

    // Chamado quando o app abre já com sessão salva, pra religar o tempo real.
    fun resumeRealtimeIfLoggedIn() = realtime.start()

    suspend fun currentUserId(): Long? = tokenStore.currentUserId()

    suspend fun setBaseUrl(url: String) = tokenStore.setBaseUrl(url)

    // --- Conversas e mensagens ---

    suspend fun listConversations(): List<ConversationSummary> = api.listConversations()

    suspend fun getConversation(otherId: Long): List<Message> = api.getConversation(otherId)

    suspend fun findUser(username: String): User = api.findUser(username)

    // Envia uma mensagem 1-a-1.
    //
    // >>> É AQUI que a criptografia E2E vai entrar no próximo passo. <<<
    // Hoje (fluxo primeiro) mandamos o texto direto no campo ciphertext. Depois,
    // vamos: pegar a chave pública do destinatário, criptografar `text` com
    // libsodium, e mandar o resultado embaralhado no lugar. O resto do app não
    // precisa mudar.
    suspend fun sendMessage(recipientId: Long, text: String): Message {
        val ciphertext = text          // TODO E2E: substituir por criptografia real
        val nonce = "plaintext"        // TODO E2E: nonce gerado pela criptografia
        return api.sendMessage(recipientId, ciphertext, nonce)
    }

    // Converte o conteúdo de uma mensagem pra texto exibível.
    // Hoje é só devolver o ciphertext (que ainda é texto puro). Depois será:
    // descriptografar com a chave privada e devolver o texto original.
    fun decryptForDisplay(message: Message): String {
        return message.ciphertext     // TODO E2E: descriptografar de verdade
    }
}
