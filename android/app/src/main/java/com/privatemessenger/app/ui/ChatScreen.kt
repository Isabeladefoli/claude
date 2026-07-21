package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.ChatViewModel

// Tela de conversa com uma pessoa. Lista as mensagens em cima e um campo pra
// escrever embaixo. Sem design: alinhamos só à esquerda/direita pra distinguir
// quem enviou.
@Composable
fun ChatScreen(
    repo: MessengerRepository,
    partnerId: Long,
    partnerUsername: String,
    onBack: () -> Unit,
) {
    val vm: ChatViewModel = viewModel(factory = chatViewModelFactory(repo, partnerId))
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    var draft by remember { mutableStateOf("") }

    // Sempre que chegar/enviar mensagem, rola pro final da lista.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

        // Cabeçalho: voltar + nome da pessoa.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Voltar") }
            Text(partnerUsername, modifier = Modifier.padding(start = 8.dp))
        }

        if (state.error != null) {
            Text("Erro: ${state.error}", modifier = Modifier.padding(vertical = 4.dp))
        }

        // Lista de mensagens (ocupa o espaço do meio; weight = 1f).
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(state.messages.size) { index ->
                val msg = state.messages[index]
                val mine = msg.senderId == state.myUserId
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                    // Mostra o remetente (eu/ele) + o texto da mensagem.
                    Text((if (mine) "eu: " else "$partnerUsername: ") + vm.displayText(msg))
                }
            }
        }

        // Campo de escrever + botão enviar.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Mensagem") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    vm.send(draft)
                    draft = ""
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("Enviar")
            }
        }
    }
}
