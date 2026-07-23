package com.privatemessenger.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.ChatItem
import com.privatemessenger.app.data.MediaMessage
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.vm.ChatFilter
import com.privatemessenger.app.vm.ConversationsViewModel

// Lista de chats (tela inicial). Tem:
//  - cabeçalho: seu nome + engrenagem (abre configurações)
//  - busca (filtra a lista dentro do filtro escolhido)
//  - botão "Tipos de chat": Todos / Salvos / Não salvos
//  - a lista em si (clicar abre; segurar apaga de "Todos")
//  - botão flutuante "+" que abre a BUSCA de usuários
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(
    repo: MessengerRepository,
    onOpenChat: (partnerId: Long, partnerUsername: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: (username: String) -> Unit,
) {
    val vm: ConversationsViewModel = viewModel(factory = conversationsViewModelFactory(repo))
    val state by vm.state.collectAsState()

    // Poll enquanto a tela está visível (para quando some).
    DisposableEffect(Unit) {
        vm.onScreenActive()
        onDispose { vm.onScreenInactive() }
    }

    var filterMenuOpen by remember { mutableStateOf(false) }
    val visible = vm.visibleChats()

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // Cabeçalho: avatar + nome à esquerda, engrenagem à direita.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(seed = state.myUsername, size = 40.dp, avatarPath = state.myAvatar)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.myUsername.ifEmpty { "..." },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                // Botão de configurações (abre o menu).
                Text(
                    "Config",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenSettings() }.padding(8.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

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
                    Text("Tipo de chat: ${filterLabel(state.filter)}")
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
                            onOpenProfile = { onOpenProfile(chat.user.username) },
                            onHide = { vm.hideChat(chat) },
                            onUnhide = { vm.unhide(chat) },
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

        // Botão flutuante "+" — abre a busca de usuários (adicionar / conversar).
        FloatingActionButton(
            onClick = { onOpenSearch() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Text("+", fontSize = 26.sp)
        }
    }
}

// Uma linha da lista. Segurar (long-press) abre o menu de apagar de Todos.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: ChatItem,
    isMe: Boolean,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = { menuOpen = true },
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                seed = chat.user.username,
                size = 44.dp,
                avatarPath = chat.user.avatarUrl,
                onClick = onOpenProfile,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                val name = buildString {
                    append(chat.user.username)
                    if (isMe) append(" (eu)")
                    if (chat.saved) append("  ★")
                }
                Text(name, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                // Prévia da última mensagem, ou marca de contato salvo sem conversa.
                // Se a última for mídia, mostramos "Foto"/"Áudio" no lugar do
                // conteúdo cru.
                val preview = chat.lastMessage?.let { m ->
                    when (MediaMessage.parse(m.ciphertext)?.first) {
                        MediaMessage.IMAGE -> "Foto"
                        MediaMessage.AUDIO -> "Áudio"
                        else -> m.ciphertext
                    }
                } ?: if (chat.saved) "Contato salvo" else ""
                if (preview.isNotEmpty()) {
                    Text(preview, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Bolinha de não-lidas (só aparece se houver mensagens novas).
            if (chat.unreadCount > 0) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        chat.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (chat.hidden) {
                DropdownMenuItem(
                    text = { Text("Colocar em “Todos”") },
                    onClick = { menuOpen = false; onUnhide() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Apagar de “Todos”") },
                    onClick = { menuOpen = false; onHide() },
                )
            }
        }
    }
}

private fun filterLabel(f: ChatFilter): String = when (f) {
    ChatFilter.TODOS -> "todos"
    ChatFilter.SALVOS -> "salvos"
    ChatFilter.NAO_SALVOS -> "não salvos"
}

private fun emptyLabel(f: ChatFilter, query: String): String {
    if (query.isNotBlank()) return "Nada encontrado para “$query”."
    return when (f) {
        ChatFilter.TODOS -> "Nenhum chat ainda. Toque no + pra começar!"
        ChatFilter.SALVOS -> "Nenhum contato salvo. Toque no + pra adicionar!"
        ChatFilter.NAO_SALVOS -> "Nenhum chat não salvo."
    }
}
