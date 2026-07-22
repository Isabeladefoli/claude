package com.privatemessenger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.ui.theme.ChatWallpaper
import com.privatemessenger.app.vm.ChatViewModel

// Tela de conversa. Mensagens em BOLHAS (estilo zap): as minhas à direita
// (azul), as da outra pessoa à esquerda. O fundo usa o papel de parede
// escolhido nas Preferências.
@Composable
fun ChatScreen(
    repo: MessengerRepository,
    partnerId: Long,
    partnerUsername: String,
    onBack: () -> Unit,
) {
    // A "key" garante um ViewModel por conversa (trocar de pessoa carrega do zero).
    val vm: ChatViewModel = viewModel(
        key = "chat-$partnerId",
        factory = chatViewModelFactory(repo, partnerId),
    )
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val wallpaper by repo.chatWallpaper.collectAsState(initial = ChatWallpaper.PADRAO)

    // Poll enquanto a tela está visível (mantém igual em todos os aparelhos).
    DisposableEffect(partnerId) {
        vm.onScreenActive()
        onDispose { vm.onScreenInactive() }
    }

    var draft by remember { mutableStateOf("") }

    // Sempre que chegar/enviar mensagem, rola pro final.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Fundo: o papel de parede escolhido, ou o fundo do tema (PADRAO).
    val bg = wallpaper.color ?: MaterialTheme.colorScheme.background

    Column(modifier = Modifier.fillMaxSize().background(bg)) {

        // Cabeçalho: voltar + avatar + nome.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(6.dp))
            Avatar(seed = partnerUsername, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            val headerLabel = if (partnerId == state.myUserId) "$partnerUsername (eu)" else partnerUsername
            Text(
                headerLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (state.error != null) {
            Text(
                "Erro: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp),
            )
        }

        // Lista de bolhas (ocupa o meio).
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
        ) {
            items(state.messages.size) { index ->
                val msg = state.messages[index]
                val mine = msg.senderId == state.myUserId
                MessageBubble(text = vm.displayText(msg), mine = mine)
            }
        }

        // Campo de escrever + botão redondo de enviar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Mensagem") },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (draft.isNotBlank()) {
                            vm.send(draft)
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("➤", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// Uma bolha de mensagem. Minhas: à direita, azul. Da outra pessoa: à esquerda,
// cinza-claro. O cantinho "sem arredondar" dá aquele biquinho de bolha.
@Composable
private fun MessageBubble(text: String, mine: Boolean) {
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val shape = if (mine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text, color = textColor, fontSize = 16.sp)
        }
    }
}
