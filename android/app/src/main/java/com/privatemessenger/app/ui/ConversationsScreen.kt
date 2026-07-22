package com.privatemessenger.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.BUILD_TAG
import com.privatemessenger.app.data.ChatItem
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.ChatFilter
import com.privatemessenger.app.vm.ConversationsViewModel
import kotlinx.coroutines.launch

// Lista de chats (tela inicial). Tem:
//  - busca (filtra a lista dentro do filtro escolhido)
//  - botão "Tipos de chat": Todos / Salvos / Não salvos
//  - a lista em si (clicar abre; segurar apaga de "Todos")
//  - botão flutuante "+" pra adicionar contato ou só conversar
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(
    repo: MessengerRepository,
    onOpenChat: (partnerId: Long, partnerUsername: String) -> Unit,
    onLogout: () -> Unit,
) {
    val vm: ConversationsViewModel = viewModel(factory = conversationsViewModelFactory(repo))
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Poll enquanto a tela está visível (para quando some).
    DisposableEffect(Unit) {
        vm.onScreenActive()
        onDispose { vm.onScreenInactive() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }

    val visible = vm.visibleChats()

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // Cabeçalho
            Text(
                "Conversas  (build $BUILD_TAG)",
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.myUsername.isNotEmpty()) {
                    Text("Logado como ${state.myUsername}")
                }
                TextButton(onClick = onLogout) { Text("Sair") }
            }

            Spacer(Modifier.height(8.dp))

            // Barra de busca (funciona como filtro dentro do tipo escolhido).
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                label = { Text("Buscar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Botão "Tipos de chat" com as três opções.
            Box {
                OutlinedButton(onClick = { filterMenuOpen = true }) {
                    Text("Tipos de chat: ${filterLabel(state.filter)}")
                }
                DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = { vm.setFilter(ChatFilter.TODOS); filterMenuOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Salvos") },
                        onClick = { vm.setFilter(ChatFilter.SALVOS); filterMenuOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Não salvos") },
                        onClick = { vm.setFilter(ChatFilter.NAO_SALVOS); filterMenuOpen = false },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Lista
            when {
                state.loading -> Text("Carregando...")
                state.error != null -> Text("Erro: ${state.error}")
                visible.isEmpty() -> Text(emptyLabel(state.filter, state.query))
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(visible, key = { it.user.id }) { chat ->
                        ChatRow(
                            chat = chat,
                            isMe = chat.user.id == state.myUserId,
                            onOpen = { onOpenChat(chat.user.id, chat.user.username) },
                            onHide = { vm.hideChat(chat) },
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Faixa de "reverter" depois de apagar um chat de Todos.
            if (state.lastHiddenId != null) {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("“${state.lastHiddenName}” apagado de Todos")
                        Row {
                            TextButton(onClick = { vm.undoHide() }) { Text("Reverter") }
                            TextButton(onClick = { vm.dismissUndo() }) { Text("OK") }
                        }
                    }
                }
            }
        }

        // Botão flutuante "+" (adicionar contato / conversar).
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Text("+", fontSize = 26.sp)
        }
    }

    if (showAddDialog) {
        AddChatDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, done ->
                vm.addContact(name) { result ->
                    if (result.isSuccess) {
                        showAddDialog = false
                    }
                    done(result.exceptionOrNull()?.message)
                }
            },
            onJustChat = { name, done ->
                scope.launch {
                    try {
                        val user = repo.findUser(name.trim())
                        showAddDialog = false
                        onOpenChat(user.id, user.username)
                    } catch (e: Exception) {
                        done("Usuário não encontrado")
                    }
                }
            },
        )
    }
}

// Uma linha da lista. Segurar (long-press) abre o menu de apagar de Todos.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: ChatItem,
    isMe: Boolean,
    onOpen: () -> Unit,
    onHide: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = { menuOpen = true },
                )
                .padding(vertical = 12.dp),
        ) {
            val name = buildString {
                append(chat.user.username)
                if (isMe) append(" (eu)")
                if (chat.saved) append("  ★")
            }
            Text(name, fontWeight = FontWeight.Bold)
            // Prévia da última mensagem, ou marca de contato salvo sem conversa.
            val preview = chat.lastMessage?.ciphertext
                ?: if (chat.saved) "Contato salvo" else ""
            if (preview.isNotEmpty()) {
                Text(preview)
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Apagar de “Todos”") },
                onClick = { menuOpen = false; onHide() },
            )
        }
    }
}

// Diálogo de adicionar/conversar por nome de usuário.
@Composable
private fun AddChatDialog(
    onDismiss: () -> Unit,
    onAdd: (username: String, done: (error: String?) -> Unit) -> Unit,
    onJustChat: (username: String, done: (error: String?) -> Unit) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar ou conversar") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("Nome de usuário") },
                    singleLine = true,
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Erro: $error")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (username.isBlank()) { error = "Digite um nome"; return@Button }
                onAdd(username) { err -> error = err }
            }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (username.isBlank()) { error = "Digite um nome"; return@TextButton }
                onJustChat(username) { err -> error = err }
            }) { Text("Só conversar") }
        },
    )
}

private fun filterLabel(f: ChatFilter): String = when (f) {
    ChatFilter.TODOS -> "Todos"
    ChatFilter.SALVOS -> "Salvos"
    ChatFilter.NAO_SALVOS -> "Não salvos"
}

private fun emptyLabel(f: ChatFilter, query: String): String {
    if (query.isNotBlank()) return "Nada encontrado para “$query”."
    return when (f) {
        ChatFilter.TODOS -> "Nenhum chat ainda. Toque no + pra começar!"
        ChatFilter.SALVOS -> "Nenhum contato salvo. Toque no + pra adicionar!"
        ChatFilter.NAO_SALVOS -> "Nenhum chat não salvo."
    }
}
