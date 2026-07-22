package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.Message
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    // Enquanto a tela do chat está visível, ficamos ressincronizando com o
    // servidor de tempos em tempos. O servidor é a fonte da verdade; o tempo
    // real (WebSocket) é só um "atalho" pra chegar instantâneo. Em emulador o
    // WebSocket falha bastante, então esse poll garante que a conversa fica
    // igual em todos os aparelhos mesmo se o tempo real não entregar.
    private var pollJob: Job? = null

    init {
        loadHistory()
        observeRealtime()
    }

    // Chamado quando a tela de chat aparece: recarrega e começa a ressincronizar.
    fun onScreenActive() {
        loadHistory()
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                loadHistory()
            }
        }
    }

    // Chamado quando a tela some: para de ressincronizar (economiza bateria/rede).
    fun onScreenInactive() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val myId = if (_state.value.myUserId != -1L) {
                    _state.value.myUserId
                } else {
                    repo.currentUserId() ?: -1
                }
                val history = repo.getConversation(partnerId)
                // Junta o que veio do servidor com o que já temos na tela,
                // sem duplicar (por id) e mantendo a ordem cronológica.
                val merged = (history + _state.value.messages)
                    .distinctBy { it.id }
                    .sortedBy { it.id }
                _state.value = _state.value.copy(
                    loading = false,
                    messages = merged,
                    myUserId = myId,
                    error = null,
                )
            } catch (e: Exception) {
                // Poll silencioso: se já temos mensagens na tela, uma falha
                // momentânea não deve apagá-las nem mostrar erro. Só mostramos
                // erro quando ainda não há nada carregado.
                if (_state.value.messages.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
            }
        }
    }

    // Escuta mensagens em tempo real e adiciona à tela SE forem desta conversa.
    private fun observeRealtime() {
        viewModelScope.launch {
            repo.incomingMessages.collect { msg ->
                val myId = _state.value.myUserId
                // Cobre os dois sentidos: mensagem que o parceiro me mandou, E
                // mensagem que EU mandei pro parceiro a partir de OUTRO
                // aparelho (multi-device: a mesma conta logada em dois
                // lugares precisa ver a conversa igual nos dois).
                val belongsHere = msg.groupId == null && (
                    (msg.senderId == partnerId && msg.recipientId == myId) ||
                    (msg.senderId == myId && msg.recipientId == partnerId)
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

    // Evita duplicar a mesma mensagem (ex: chegou pelo envio E pelo WebSocket)
    // e mantém a lista ordenada por id (ordem cronológica).
    private fun appendUnique(msg: Message) {
        val current = _state.value.messages
        if (current.any { it.id == msg.id }) return
        _state.value = _state.value.copy(messages = (current + msg).sortedBy { it.id })
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
