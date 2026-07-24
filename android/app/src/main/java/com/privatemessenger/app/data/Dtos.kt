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
    val name: String? = null,
    val birthday: String? = null,
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
    val name: String? = null,
    val birthday: String? = null,
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
data class EditMessageRequest(
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

// --- Contatos e lista de chats (todos/salvos/não salvos) ---

// Cartão público de um usuário (o que aparece numa linha de chat).
@Serializable
data class PublicUser(
    val id: Long,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

// Uma linha da lista de chats, com os marcadores que decidem em qual filtro
// ela aparece.
@Serializable
data class ChatItem(
    val user: PublicUser,
    val saved: Boolean = false,
    val hidden: Boolean = false,
    @SerialName("has_messages") val hasMessages: Boolean = false,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("last_message") val lastMessage: Message? = null,
)

@Serializable
data class BlocksResponse(
    val blocked: List<Long> = emptyList(),
)

@Serializable
data class ReportRequest(
    val reason: String,
)

@Serializable
data class ChatsResponse(
    val chats: List<ChatItem>,
)

@Serializable
data class AddContactRequest(
    val username: String,
)

// --- Conta (tela Account details) ---

@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val name: String? = null,
    val birthday: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

// Usado tanto pra "conferir senha" (trava da tela) quanto pra apagar conta.
@Serializable
data class PasswordRequest(
    val password: String,
)

// --- Suporte ---

@Serializable
data class SupportRequest(
    @SerialName("reply_email") val replyEmail: String,
    val title: String,
    val body: String,
)

// --- Mídia ---

// Resposta do upload: id + caminho (url relativa) do arquivo no servidor.
@Serializable
data class MediaResponse(
    val id: String,
    val url: String, // ex: "/api/media/abc123" — o cliente junta com a baseUrl
    val mime: String? = null,
    val size: Long? = null,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

@Serializable
data class DeviceTokenRequest(
    val token: String,
)

// Envelope do WebSocket: { "type": "message", "data": {...} }. O "type" diz o
// que chegou; "data" carrega o conteúdo (uma Message, no caso de mensagens).
@Serializable
data class WsEnvelope(
    val type: String,
    val data: kotlinx.serialization.json.JsonElement,
)
