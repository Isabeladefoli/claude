package com.privatemessenger.app.data

import android.content.Context
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.ui.theme.FontSize
import com.privatemessenger.app.ui.theme.ThemeMode
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

    // Preferências de aparência/idioma (a interface observa e reage na hora).
    val themeMode: Flow<ThemeMode> = tokenStore.themeMode
    val fontSize: Flow<FontSize> = tokenStore.fontSize
    val language: Flow<AppLanguage> = tokenStore.language

    suspend fun setThemeMode(mode: ThemeMode) = tokenStore.setThemeMode(mode)
    suspend fun setFontSize(size: FontSize) = tokenStore.setFontSize(size)
    suspend fun setLanguage(lang: AppLanguage) = tokenStore.setLanguage(lang)

    // --- Autenticação ---

    suspend fun register(
        username: String,
        password: String,
        publicKey: String,
        name: String? = null,
        birthday: String? = null,
    ): AuthResponse {
        val auth = api.register(RegisterRequest(username, password, publicKey, name, birthday))
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

    // Busca o username de quem tá logado (pra mostrar "Logado como {user}").
    suspend fun currentUsername(): String = api.me().username

    // Perfil completo do usuário logado (nome, aniversário, etc).
    suspend fun me(): User = api.me()

    // --- Conta (tela Account details) ---

    suspend fun verifyPassword(password: String): Boolean = api.verifyPassword(password)

    suspend fun updateProfile(
        username: String? = null,
        name: String? = null,
        birthday: String? = null,
        avatarUrl: String? = null,
    ): User = api.updateProfile(UpdateProfileRequest(username, name, birthday, avatarUrl))

    suspend fun changePassword(current: String, new: String) = api.changePassword(current, new)

    // Apaga a conta no servidor E limpa a sessão local (não sobra token de conta
    // que não existe mais).
    suspend fun deleteAccount(password: String) {
        api.deleteAccount(password)
        realtime.stop()
        tokenStore.clear()
    }

    // --- Suporte ---

    suspend fun sendSupport(replyEmail: String, title: String, body: String) =
        api.sendSupport(replyEmail, title, body)

    // --- Mídia ---

    // Sobe um arquivo e devolve o caminho ("/api/media/xxx") pra referenciar.
    suspend fun uploadMedia(bytes: ByteArray, filename: String, mime: String): String =
        api.uploadMedia(bytes, filename, mime).url

    // Junta a URL do servidor com o caminho da mídia, pra formar o endereço
    // completo que o carregador de imagem (Coil) usa. Ex: baseUrl + "/api/media/x".
    suspend fun mediaFullUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return tokenStore.currentBaseUrl().trimEnd('/') + path
    }

    suspend fun setBaseUrl(url: String) = tokenStore.setBaseUrl(url)

    // Lê a URL do servidor uma vez (pra preencher o campo na tela de login).
    suspend fun currentBaseUrl(): String = tokenStore.currentBaseUrl()

    // --- Conversas e mensagens ---

    suspend fun listConversations(): List<ConversationSummary> = api.listConversations()

    suspend fun getConversation(otherId: Long): List<Message> = api.getConversation(otherId)

    suspend fun findUser(username: String): User = api.findUser(username)

    // --- Contatos e chats (todos/salvos/não salvos) ---

    suspend fun listChats(): List<ChatItem> = api.listChats()

    suspend fun addContact(username: String): PublicUser = api.addContact(username)

    suspend fun hideChat(otherId: Long) = api.hideChat(otherId)

    suspend fun unhideChat(otherId: Long) = api.unhideChat(otherId)

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
