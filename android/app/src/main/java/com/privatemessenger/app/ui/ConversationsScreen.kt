package com.privatemessenger.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.ConversationsViewModel
import kotlinx.coroutines.launch

// Lista de conversas (tela inicial depois de logar).
// - Em cima: iniciar conversa nova digitando o nome de usuário de alguém.
// - No meio: lista de conversas existentes (clicar abre o chat).
// - Sair: encerra a sessão.
@Composable
fun ConversationsScreen(
    repo: MessengerRepository,
    onOpenChat: (partnerId: Long, partnerUsername: String) -> Unit,
    onLogout: () -> Unit,
) {
    val vm: ConversationsViewModel = viewModel(factory = conversationsViewModelFactory(repo))
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Enquanto essa tela está visível, ressincroniza a lista com o servidor
    // (poll). Para quando a tela some. Assim novas conversas/mensagens aparecem
    // sozinhas mesmo que o tempo real (WebSocket) falhe.
    DisposableEffect(Unit) {
        vm.onScreenActive()
        onDispose { vm.onScreenInactive() }
    }

    var newUsername by remember { mutableStateOf("") }
    var startError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Conversas", modifier = Modifier.padding(bottom = 8.dp))
        }

        if (state.myUsername.isNotEmpty()) {
            Text(
                "Logado como ${state.myUsername}",
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Iniciar conversa nova.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Usuário para conversar") },
                singleLine = true,
                modifier = Modifier.padding(end = 8.dp),
            )
            Button(onClick = {
                val name = newUsername.trim()
                if (name.isEmpty()) return@Button
                scope.launch {
                    try {
                        val user = repo.findUser(name)
                        startError = null
                        newUsername = ""
                        onOpenChat(user.id, user.username)
                    } catch (e: Exception) {
                        startError = "Usuário não encontrado"
                    }
                }
            }) {
                Text("Abrir")
            }
        }

        if (startError != null) {
            Text(startError!!, modifier = Modifier.padding(top = 4.dp))
        }

        TextButton(onClick = onLogout, modifier = Modifier.padding(top = 4.dp)) {
            Text("Sair")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        when {
            state.loading -> Text("Carregando...")
            state.error != null -> Text("Erro: ${state.error}")
            state.conversations.isEmpty() -> Text("Nenhuma conversa ainda. Comece uma acima!")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.conversations) { conv ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(conv.partnerId, conv.partnerUsername) }
                            .padding(vertical = 12.dp),
                    ) {
                        val label = if (conv.partnerId == state.myUserId) {
                            "${conv.partnerUsername} (eu)"
                        } else {
                            conv.partnerUsername
                        }
                        Text(label)
                        // Prévia da última mensagem (por enquanto texto puro).
                        Text(conv.lastMessage.ciphertext)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
