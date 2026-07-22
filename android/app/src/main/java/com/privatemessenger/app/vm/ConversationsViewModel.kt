package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.ChatItem
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Os três filtros da lista de chats.
enum class ChatFilter { TODOS, SALVOS, NAO_SALVOS }

// Estado da tela de lista de conversas.
data class ConversationsUiState(
    val loading: Boolean = false,
    val chats: List<ChatItem> = emptyList(),
    val error: String? = null,
    val myUserId: Long = -1,
    val myUsername: String = "",
    val filter: ChatFilter = ChatFilter.TODOS,
    val query: String = "",
    // Guarda o último chat "apagado de Todos" pra oferecer o "reverter".
    val lastHiddenId: Long? = null,
    val lastHiddenName: String? = null,
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

    // ---- Filtro e busca (tudo no cliente, instantâneo) ----

    fun setFilter(f: ChatFilter) {
        _state.value = _state.value.copy(filter = f)
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    // A lista JÁ FILTRADA que a tela deve mostrar, aplicando filtro + busca.
    //
    // Regras:
    //  - TODOS:      quem é salvo OU tem mensagem
    //  - SALVOS:     só os salvos
    //  - NAO_SALVOS: quem NÃO é salvo mas tem mensagem
    //  - Sem busca: em TODOS e NÃO_SALVOS escondemos os "apagados de Todos"
    //    (hidden). Em SALVOS eles continuam aparecendo (salvar é um "manter").
    //  - Com busca: procuramos pelo nome, REVELANDO até os hidden (a busca acha
    //    tudo, é assim que você reencontra um chat que apagou de Todos).
    fun visibleChats(): List<ChatItem> {
        val s = _state.value
        val base = when (s.filter) {
            ChatFilter.TODOS -> s.chats.filter { it.saved || it.hasMessages }
            ChatFilter.SALVOS -> s.chats.filter { it.saved }
            ChatFilter.NAO_SALVOS -> s.chats.filter { !it.saved && it.hasMessages }
        }
        val q = s.query.trim()
        return if (q.isBlank()) {
            if (s.filter == ChatFilter.SALVOS) base else base.filter { !it.hidden }
        } else {
            base.filter { it.user.username.contains(q, ignoreCase = true) }
        }
    }

    // ---- Ações ----

    // Adiciona (salva) um contato pelo nome de usuário.
    fun addContact(username: String, onDone: (Result<Unit>) -> Unit) {
        val name = username.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            try {
                repo.addContact(name)
                refresh()
                onDone(Result.success(Unit))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }

    // Apaga um chat da lista Todos (guarda pra poder reverter).
    fun hideChat(chat: ChatItem) {
        viewModelScope.launch {
            try {
                repo.hideChat(chat.user.id)
                _state.value = _state.value.copy(
                    lastHiddenId = chat.user.id,
                    lastHiddenName = chat.user.username,
                )
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    // Reverte o último "apagar de Todos".
    fun undoHide() {
        val id = _state.value.lastHiddenId ?: return
        viewModelScope.launch {
            try {
                repo.unhideChat(id)
                _state.value = _state.value.copy(lastHiddenId = null, lastHiddenName = null)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun dismissUndo() {
        _state.value = _state.value.copy(lastHiddenId = null, lastHiddenName = null)
    }

    // Busca o usuário logado uma vez só (pro "Logado como {user}" no topo).
    private fun loadMe() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    myUsername = repo.currentUsername(),
                    myUserId = repo.currentUserId() ?: -1,
                )
            } catch (_: Exception) {
                // Sem problema deixar em branco se falhar; não é crítico.
            }
        }
    }

    // Carrega a lista de chats do servidor — de forma SILENCIOSA (sem piscar
    // "Carregando" a cada poll). O spinner só aparece na primeira carga.
    fun refresh() {
        viewModelScope.launch {
            try {
                val myId = repo.currentUserId() ?: -1
                val chats = repo.listChats()
                _state.value = _state.value.copy(
                    loading = false,
                    chats = chats,
                    myUserId = myId,
                    error = null,
                )
            } catch (e: Exception) {
                if (_state.value.chats.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = e.message)
                }
            }
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            repo.incomingMessages.collect { refresh() }
        }
    }

    // Poll enquanto a tela está visível — não depender só do WebSocket.
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
