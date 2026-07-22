package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.ConversationSummary
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Estado da tela de lista de conversas.
data class ConversationsUiState(
    val loading: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val error: String? = null,
    val myUserId: Long = -1,
    val myUsername: String = "",
)

class ConversationsViewModel(private val repo: MessengerRepository) : ViewModel() {

    // Começa em "loading" pra não piscar "Nenhuma conversa" antes da primeira
    // carga terminar (isso assustava: parecia que as conversas tinham sumido).
    private val _state = MutableStateFlow(ConversationsUiState(loading = true))
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    init {
        loadMe()
        refresh()
        observeRealtime()
    }

    // Busca o usuário logado uma vez só (pro "Logado como {user}" no topo).
    private fun loadMe() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(myUsername = repo.currentUsername())
            } catch (_: Exception) {
                // Sem problema deixar em branco se falhar; não é crítico pra tela funcionar.
            }
        }
    }

    // Carrega a lista de conversas do servidor.
    //
    // Repare: NÃO ligamos "loading = true" aqui. O spinner de carregamento só
    // aparece na primeira vez (o estado já nasce com loading = true), e some
    // quando os dados chegam. Como o refresh roda a cada 4s por baixo, ligar o
    // loading toda vez faria "Carregando" piscar na tela — péssimo. Então o
    // refresh atualiza os dados de forma SILENCIOSA.
    fun refresh() {
        viewModelScope.launch {
            try {
                val myId = repo.currentUserId() ?: -1
                val list = repo.listConversations()
                _state.value = _state.value.copy(
                    loading = false,
                    conversations = list,
                    myUserId = myId,
                    error = null,
                )
            } catch (e: Exception) {
                // Só mostra erro se a tela ainda está vazia. Se já temos
                // conversas na tela, uma falha momentânea de um poll não deve
                // apagar tudo e mostrar erro — mantemos o que já está exibido.
                if (_state.value.conversations.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
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

    // Poll enquanto a tela está visível — mesma ideia do chat: não depender só
    // do WebSocket, que é instável no emulador.
    private var pollJob: Job? = null

    fun onScreenActive() {
        refresh()
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                refresh()
            }
        }
    }

    fun onScreenInactive() {
        pollJob?.cancel()
        pollJob = null
    }
}
