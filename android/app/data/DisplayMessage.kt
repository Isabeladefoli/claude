package com.privatemessenger.app.data

// Uma mensagem já descriptografada e pronta pra exibição.
data class DisplayMessage(
    val id: Long,
    val senderId: Long,
    val recipientId: Long?,
    val groupId: Long?,
    val text: String, // texto já descriptografado (ou URL de mídia)
    val createdAt: String,
)

fun Message.toDisplay(decryptedText: String): DisplayMessage {
    return DisplayMessage(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        groupId = groupId,
        text = decryptedText,
        createdAt = createdAt,
    )
}
