package com.privatemessenger.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// DTOs (Data Transfer Objects): classes Kotlin que espelham EXATAMENTE o JSON
// que o servidor Go manda e recebe. A biblioteca kotlinx.serialization converte
// automaticamente entre estas classes e o JSON.
//
// Detalhe: no servidor os campos usam snake_case (ex: "user_id"), e em Kotlin
// usamos camelCase (userId). O @SerialName faz a ponte entre os dois nomes.
// ---------------------------------------------------------------------------

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    @SerialName("public_key") val publicKey: String,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("user_id") val userId: Long,
)

@Serializable
data class User(
    val id: Long,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("public_key") val publicKey: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// Uma mensagem. Repare: o conteúdo é "ciphertext" (texto criptografado), não
// texto puro. Por enquanto (fluxo primeiro) mandamos o texto simples aqui; a
// criptografia real entra no próximo passo, sem mudar este formato.
@Serializable
data class Message(
    val id: Long,
    @SerialName("sender_id") val senderId: Long,
    @SerialName("recipient_id") val recipientId: Long? = null,
    @SerialName("group_id") val groupId: Long? = null,
    val ciphertext: String,
    val nonce: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SendMessageRequest(
    @SerialName("recipient_id") val recipientId: Long,
    val ciphertext: String,
    val nonce: String,
)

@Serializable
data class ConversationSummary(
    @SerialName("partner_id") val partnerId: Long,
    @SerialName("partner_username") val partnerUsername: String,
    @SerialName("partner_avatar") val partnerAvatar: String? = null,
    @SerialName("last_message") val lastMessage: Message,
)

@Serializable
data class ConversationsResponse(
    val conversations: List<ConversationSummary>,
)

@Serializable
data class MessagesResponse(
    val messages: List<Message>,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

// Envelope do WebSocket: { "type": "message", "data": {...} }. O "type" diz o
// que chegou; "data" carrega o conteúdo (uma Message, no caso de mensagens).
@Serializable
data class WsEnvelope(
    val type: String,
    val data: kotlinx.serialization.json.JsonElement,
)
