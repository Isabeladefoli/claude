package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.ConversationSummary
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado da tela de lista de conversas.
data class ConversationsUiState(
    val loading: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val error: String? = null,
    val myUserId: Long = -1,
)

class ConversationsViewModel(private val repo: MessengerRepository) : ViewModel() {

    private val _state = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    init {
        refresh()
        observeRealtime()
    }

    // Carrega a lista de conversas do servidor.
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val myId = repo.currentUserId() ?: -1
                val list = repo.listConversations()
                _state.value = ConversationsUiState(conversations = list, myUserId = myId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    // Quando chega mensagem nova em tempo real, recarrega a lista pra atualizar
    // a "última mensagem" e a ordem. (Simples e suficiente por enquanto.)
    private fun observeRealtime() {
        viewModelScope.launch {
            repo.incomingMessages.collect { refresh() }
        }
    }
}
