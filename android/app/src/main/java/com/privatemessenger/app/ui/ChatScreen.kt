package com.privatemessenger.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MediaMessage
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.ui.components.AudioBubble
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.ui.components.FullscreenImage
import com.privatemessenger.app.ui.components.LocalBaseUrl
import com.privatemessenger.app.ui.components.rememberNetworkImage
import com.privatemessenger.app.ui.theme.ChatWallpaper
import com.privatemessenger.app.vm.ChatViewModel

// Tela de conversa. Mensagens em BALÕES; dá pra enviar foto/áudio, apagar e
// copiar mensagem (segurando o balão), e tocar no avatar pra abrir o perfil.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    repo: MessengerRepository,
    partnerId: Long,
    partnerUsername: String,
    onBack: () -> Unit,
    onOpenProfile: (username: String) -> Unit,
) {
    val vm: ChatViewModel = viewModel(
        key = "chat-$partnerId",
        factory = chatViewModelFactory(repo, partnerId),
    )
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val wallpaper by repo.chatWallpaper.collectAsState(initial = ChatWallpaper.PADRAO)
    val context = LocalContext.current

    DisposableEffect(partnerId) {
        vm.onScreenActive()
        onDispose { vm.onScreenInactive() }
    }

    var draft by remember { mutableStateOf("") }
    var attachMenu by remember { mutableStateOf(false) }
    var expandUrl by remember { mutableStateOf<String?>(null) }

    // Estado da gravação de áudio.
    val recorder = remember { com.privatemessenger.app.ui.components.AudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }

    // Seletor de foto (galeria) — não precisa de permissão.
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) vm.sendMedia(bytes, "photo.jpg", mime, MediaMessage.IMAGE)
        }
    }

    // Permissão de microfone: se concedida, começa a gravar.
    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) recording = recorder.start()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val bg = wallpaper.color ?: MaterialTheme.colorScheme.background

    Column(modifier = Modifier.fillMaxSize().background(bg)) {

        // Cabeçalho: voltar + avatar/nome (tocar abre o perfil da pessoa).
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
            Avatar(seed = partnerUsername, size = 38.dp, onClick = { onOpenProfile(partnerUsername) })
            Spacer(Modifier.width(10.dp))
            val headerLabel = if (partnerId == state.myUserId) "$partnerUsername (eu)" else partnerUsername
            Text(
                headerLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onOpenProfile(partnerUsername) },
            )
        }

        if (state.error != null) {
            Text(
                "Erro: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
        ) {
            items(state.messages.size) { index ->
                val msg = state.messages[index]
                val mine = msg.senderId == state.myUserId
                MessageBubble(
                    text = vm.displayText(msg),
                    mine = mine,
                    onDelete = { vm.deleteMessage(msg) },
                    onExpandImage = { url -> expandUrl = url },
                )
            }
        }

        // Barra de baixo: ou está gravando, ou é o campo normal.
        if (recording) {
            RecordingBar(
                onSend = {
                    recording = false
                    val bytes = recorder.stop()
                    if (bytes != null) vm.sendMedia(bytes, "audio.m4a", "audio/mp4", MediaMessage.AUDIO)
                },
                onCancel = {
                    recording = false
                    recorder.cancel()
                },
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Botão de anexar (+) com menu Foto/Áudio.
                Box {
                    RoundButton("+", MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurface) { attachMenu = true }
                    DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Foto") },
                            onClick = {
                                attachMenu = false
                                pickPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Áudio") },
                            onClick = {
                                attachMenu = false
                                askMic.launch(android.Manifest.permission.RECORD_AUDIO)
                            },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Mensagem") },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RoundButton(">", MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary) {
                    if (draft.isNotBlank()) {
                        vm.send(draft)
                        draft = ""
                    }
                }
            }
        }
    }

    // Foto em tela cheia (quando toca numa imagem do chat).
    expandUrl?.let { url ->
        FullscreenImage(url = url, onDismiss = { expandUrl = null })
    }
}

// Barra que aparece durante a gravação de áudio.
@Composable
private fun RecordingBar(onSend: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Gravando áudio...", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        Row {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            TextButton(onClick = onSend) { Text("Enviar") }
        }
    }
}

// Botão redondo simples (usado no anexar e no enviar).
@Composable
private fun RoundButton(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// Um balão de conversa. Segurar abre o menu (copiar/apagar). Renderiza texto,
// foto (toca pra expandir) ou áudio, conforme o conteúdo.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    text: String,
    mine: Boolean,
    onDelete: () -> Unit,
    onExpandImage: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var menuOpen by remember { mutableStateOf(false) }

    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val shape = if (mine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val media = MediaMessage.parse(text)
    val base = LocalBaseUrl.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                when {
                    media != null && media.first == MediaMessage.IMAGE -> {
                        val url = base.trimEnd('/') + media.second
                        val photo = rememberNetworkImage(url)
                        if (photo != null) {
                            Image(
                                bitmap = photo,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onExpandImage(url) },
                            )
                        } else {
                            Text("carregando foto...", color = textColor)
                        }
                    }
                    media != null && media.first == MediaMessage.AUDIO -> {
                        val url = base.trimEnd('/') + media.second
                        AudioBubble(url = url, contentColor = textColor, buttonBg = bubbleColor)
                    }
                    else -> {
                        Text(text, color = textColor, fontSize = 16.sp)
                    }
                }
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                // Copiar só faz sentido pra texto.
                if (media == null) {
                    DropdownMenuItem(
                        text = { Text("Copiar") },
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            menuOpen = false
                        },
                    )
                }
                // Apagar só as minhas mensagens.
                if (mine) {
                    DropdownMenuItem(
                        text = { Text("Apagar") },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
