package com.privatemessenger.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
