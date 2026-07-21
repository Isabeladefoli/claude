package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.Message
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado da tela de conversa (chat) com uma pessoa específica.
data class ChatUiState(
    val loading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
    val myUserId: Long = -1,
)

// O ChatViewModel precisa saber com QUEM é a conversa (partnerId).
class ChatViewModel(
    private val repo: MessengerRepository,
    private val partnerId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(loading = true))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        loadHistory()
        observeRealtime()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val myId = repo.currentUserId() ?: -1
                val history = repo.getConversation(partnerId)
                _state.value = ChatUiState(messages = history, myUserId = myId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    // Escuta mensagens em tempo real e adiciona à tela SE forem desta conversa.
    private fun observeRealtime() {
        viewModelScope.launch {
            repo.incomingMessages.collect { msg ->
                val myId = _state.value.myUserId
                val belongsHere = msg.groupId == null && (
                    msg.senderId == partnerId && msg.recipientId == myId
                )
                if (belongsHere) {
                    appendUnique(msg)
                }
            }
        }
    }

    // Envia uma mensagem e já a mostra na tela (otimista).
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val sent = repo.sendMessage(partnerId, trimmed)
                appendUnique(sent)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    // Converte o conteúdo (por enquanto texto puro) pra exibição.
    fun displayText(msg: Message): String = repo.decryptForDisplay(msg)

    // Evita duplicar a mesma mensagem (ex: chegou pelo envio E pelo WebSocket).
    private fun appendUnique(msg: Message) {
        val current = _state.value.messages
        if (current.any { it.id == msg.id }) return
        _state.value = _state.value.copy(messages = current + msg)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
