package com.privatemessenger.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// ApiClient concentra TODA a comunicação HTTP com o servidor Go. Cada método
// aqui corresponde a uma rota da API.
//
// Usamos Ktor (cliente multiplataforma) pra que, ao migrar pra KMP no futuro,
// este arquivo seja reaproveitado quase sem mudança no Android e no iOS.
// ---------------------------------------------------------------------------

// Exceção simples pra carregar a mensagem de erro que o servidor devolveu.
class ApiException(val status: Int, message: String) : Exception(message)

class ApiClient(private val tokenStore: TokenStore) {

    // Configuração do JSON: ignora campos que não conhecemos (evita quebrar se
    // o servidor adicionar um campo novo no futuro).
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // O cliente HTTP em si. Reutilizado em todas as chamadas.
    val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(WebSockets) // pra conexão em tempo real (RealtimeClient usa isto)
        expectSuccess = false // não lança exceção automática em erro; tratamos na mão
    }

    // Monta a URL completa a partir da base configurada + o caminho.
    private suspend fun url(path: String): String {
        val base = tokenStore.currentBaseUrl().trimEnd('/')
        return "$base$path"
    }

    // Lê o corpo de erro do servidor (formato { "error": "..." }) e transforma
    // numa exceção com a mensagem amigável.
    private suspend fun fail(resp: HttpResponse): Nothing {
        val text = resp.bodyAsText()
        val msg = runCatching { json.decodeFromString<ErrorResponse>(text).error }
            .getOrDefault(text.ifBlank { "erro ${resp.status.value}" })
        throw ApiException(resp.status.value, msg)
    }

    // --- Rotas públicas ---

    suspend fun register(req: RegisterRequest): AuthResponse {
        val resp = http.post(url("/api/register")) {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status != HttpStatusCode.Created) fail(resp)
        val auth: AuthResponse = resp.body()
        tokenStore.saveSession(auth.token, auth.userId)
        return auth
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val resp = http.post(url("/api/login")) {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        val auth: AuthResponse = resp.body()
        tokenStore.saveSession(auth.token, auth.userId)
        return auth
    }

    // --- Rotas protegidas (precisam do token no cabeçalho) ---

    // Helper que anexa "Authorization: Bearer <token>" nas chamadas protegidas.
    private suspend fun authHeader(): String {
        val token = tokenStore.currentToken()
            ?: throw ApiException(401, "não autenticado")
        return "Bearer $token"
    }

    suspend fun me(): User {
        val resp = http.get(url("/api/me")) { header("Authorization", authHeader()) }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        return resp.body()
    }

    suspend fun findUser(username: String): User {
        val resp = http.get(url("/api/users/$username")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        return resp.body()
    }

    suspend fun listConversations(): List<ConversationSummary> {
        val resp = http.get(url("/api/conversations")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        return resp.body<ConversationsResponse>().conversations
    }

    suspend fun getConversation(otherId: Long): List<Message> {
        val resp = http.get(url("/api/conversations/$otherId")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        // O servidor devolve as mensagens da mais nova pra mais antiga; invertemos
        // pra exibir em ordem cronológica (antigas em cima, novas embaixo).
        return resp.body<MessagesResponse>().messages.reversed()
    }

    // --- Contatos e lista de chats ---

    suspend fun listChats(): List<ChatItem> {
        val resp = http.get(url("/api/chats")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        return resp.body<ChatsResponse>().chats
    }

    suspend fun addContact(username: String): PublicUser {
        val resp = http.post(url("/api/contacts")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(AddContactRequest(username))
        }
        if (resp.status != HttpStatusCode.Created) fail(resp)
        return resp.body()
    }

    // "Apaga" o chat da lista Todos (continua em Salvos e na busca).
    suspend fun hideChat(otherId: Long) {
        val resp = http.post(url("/api/chats/$otherId/hide")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
    }

    // Desfaz o hide (reverter).
    suspend fun unhideChat(otherId: Long) {
        val resp = http.post(url("/api/chats/$otherId/unhide")) {
            header("Authorization", authHeader())
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
    }

    // --- Conta (tela Account details) ---

    // Confere a senha atual (trava pra entrar na tela). true = senha certa.
    suspend fun verifyPassword(password: String): Boolean {
        val resp = http.post(url("/api/me/verify")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(PasswordRequest(password))
        }
        return resp.status == HttpStatusCode.OK
    }

    suspend fun updateProfile(req: UpdateProfileRequest): User {
        val resp = http.post(url("/api/me/update")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
        return resp.body()
    }

    suspend fun changePassword(current: String, new: String) {
        val resp = http.post(url("/api/me/password")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(current, new))
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
    }

    suspend fun deleteAccount(password: String) {
        val resp = http.post(url("/api/me/delete")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(PasswordRequest(password))
        }
        if (resp.status != HttpStatusCode.OK) fail(resp)
    }

    // --- Mídia ---

    // Sobe um arquivo (multipart, campo "file") e devolve o id/url dele.
    suspend fun uploadMedia(bytes: ByteArray, filename: String, mime: String): MediaResponse {
        val resp = http.post(url("/api/media")) {
            header("Authorization", authHeader())
            setBody(MultiPartFormDataContent(formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mime)
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }))
        }
        if (resp.status != HttpStatusCode.Created) fail(resp)
        return resp.body()
    }

    // --- Suporte ---

    suspend fun sendSupport(replyEmail: String, title: String, body: String) {
        val resp = http.post(url("/api/support")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(SupportRequest(replyEmail, title, body))
        }
        if (resp.status != HttpStatusCode.Created) fail(resp)
    }

    suspend fun sendMessage(recipientId: Long, ciphertext: String, nonce: String): Message {
        val resp = http.post(url("/api/messages")) {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(recipientId, ciphertext, nonce))
        }
        if (resp.status != HttpStatusCode.Created) fail(resp)
        return resp.body()
    }
}
