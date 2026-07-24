package com.privatemessenger.app.data

import android.content.Context
import com.privatemessenger.app.crypto.CryptoUtils
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.ui.theme.ChatWallpaper
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
    private val deviceKeys = DeviceKeys.create(appContext)

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
    val chatWallpaper: Flow<ChatWallpaper> = tokenStore.chatWallpaper

    suspend fun setThemeMode(mode: ThemeMode) = tokenStore.setThemeMode(mode)
    suspend fun setFontSize(size: FontSize) = tokenStore.setFontSize(size)
    suspend fun setLanguage(lang: AppLanguage) = tokenStore.setLanguage(lang)
    suspend fun setChatWallpaper(w: ChatWallpaper) = tokenStore.setChatWallpaper(w)

    // --- Autenticação ---

    suspend fun register(
        username: String,
        password: String,
        publicKey: String? = null,
        name: String? = null,
        birthday: String? = null,
    ): AuthResponse {
        // Se não passou public key, gera e salva um novo par de chaves.
        val key = publicKey ?: deviceKeys.publicKey()
        val auth = api.register(RegisterRequest(username, password, key, name, birthday))
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

    // Busca a chave pública de um usuário pelo ID (necessário pra descriptografar).
    suspend fun findUserById(userId: Long): User = api.findUser(userId.toString())

    // --- Contatos e chats (todos/salvos/não salvos) ---

    suspend fun listChats(): List<ChatItem> = api.listChats()

    suspend fun addContact(username: String): PublicUser = api.addContact(username)

    suspend fun removeContact(otherId: Long) = api.removeContact(otherId)

    suspend fun hideChat(otherId: Long) = api.hideChat(otherId)

    suspend fun unhideChat(otherId: Long) = api.unhideChat(otherId)

    suspend fun markChatRead(otherId: Long) = api.markChatRead(otherId)

    // --- Moderação (bloquear / denunciar) ---

    suspend fun listBlocks(): List<Long> = api.listBlocks()
    suspend fun isBlocked(id: Long): Boolean = api.listBlocks().contains(id)
    suspend fun blockUser(id: Long) = api.blockUser(id)
    suspend fun unblockUser(id: Long) = api.unblockUser(id)
    suspend fun reportUser(id: Long, reason: String) = api.reportUser(id, reason)

    // Envia uma mensagem 1-a-1, criptografada com E2E.
    // 1. Busca a chave pública do destinatário (para derivar a chave compartilhada)
    // 2. Criptografa o texto com AES-256-GCM via ECDH
    // 3. Envia ciphertext + nonce
    suspend fun sendMessage(recipientId: Long, text: String): Message {
        val recipient = api.findUser(recipientId.toString()) ?: throw ApiException(404, "destinatário não encontrado")
        val senderPrivateKey = deviceKeys.privateKey() ?: throw ApiException(500, "chave privada não disponível")

        val encrypted = CryptoUtils.encrypt(
            plaintext = text,
            recipientPublicKeyBase64 = recipient.publicKey,
            senderPrivateKeyBase64 = senderPrivateKey,
        )

        return api.sendMessage(recipientId, encrypted.ciphertext, encrypted.nonce)
    }

    // Apaga uma mensagem (só a própria, o servidor confere).
    suspend fun deleteMessage(id: Long) = api.deleteMessage(id)

    // Edita o texto de uma mensagem (só a própria, criptografada).
    suspend fun editMessage(id: Long, text: String) {
        val senderPrivateKey = deviceKeys.privateKey() ?: throw ApiException(500, "chave privada não disponível")
        // Assumimos que o usuário que tá editando é o que mandou (verificado no servidor).
        // Pegamos a chave pública dele mesmo pra fazer ECDH com a privada (simétrico).
        val me = me()
        val encrypted = CryptoUtils.encrypt(
            plaintext = text,
            recipientPublicKeyBase64 = me.publicKey,
            senderPrivateKeyBase64 = senderPrivateKey,
        )
        api.editMessage(id, encrypted.ciphertext, encrypted.nonce)
    }

    // Sobe um arquivo e manda como mensagem de mídia (foto/áudio) para a conversa, criptografada.
    suspend fun sendMediaMessage(
        recipientId: Long,
        bytes: ByteArray,
        filename: String,
        mime: String,
        kind: String,
    ): Message {
        val path = api.uploadMedia(bytes, filename, mime).url
        val mediaText = MediaMessage.encode(kind, path)
        return sendMessage(recipientId, mediaText)
    }

    // Descriptografa uma mensagem pra exibição.
    // Se o texto for de mídia (foto/áudio), retorna a URI encodada.
    suspend fun decryptForDisplay(message: Message): String {
        try {
            val senderPublicKey = if (message.senderId > 0) {
                findUserById(message.senderId).publicKey
            } else {
                return message.ciphertext // fallback: texto puro
            }

            val myPrivateKey = deviceKeys.privateKey() ?: return message.ciphertext

            return CryptoUtils.decrypt(
                ciphertext = message.ciphertext,
                nonce = message.nonce,
                senderPublicKeyBase64 = senderPublicKey,
                recipientPrivateKeyBase64 = myPrivateKey,
            )
        } catch (e: Exception) {
            // Se descriptografar falhar, retorna um aviso.
            return "[Erro ao descriptografar: ${e.message}]"
        }
    }
}
