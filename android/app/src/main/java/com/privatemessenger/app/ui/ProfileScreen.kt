package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.LaunchedEffect
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.data.User
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.ui.components.FullscreenImage
import com.privatemessenger.app.ui.components.LocalBaseUrl
import com.privatemessenger.app.ui.components.PrimaryButton
import com.privatemessenger.app.ui.components.TopBar
import kotlinx.coroutines.launch

// Página de perfil de um usuário: foto grande (toca pra expandir), nome de
// exibição, @usuário e ações — Mensagem, e Adicionar/Excluir contato.
@Composable
fun ProfileScreen(
    repo: MessengerRepository,
    username: String,
    onBack: () -> Unit,
    onOpenChat: (userId: Long, username: String) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val base = LocalBaseUrl.current

    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isBlockedState by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var expand by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    // Carrega o perfil + descobre se já é contato salvo e se está bloqueado.
    LaunchedEffect(username) {
        loading = true
        notFound = false
        try {
            val u = repo.findUser(username)
            user = u
            isSaved = try {
                repo.listChats().any { it.user.id == u.id && it.saved }
            } catch (_: Exception) { false }
            isBlockedState = try { repo.isBlocked(u.id) } catch (_: Exception) { false }
        } catch (_: Exception) {
            notFound = true
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(onBack = onBack, title = "")

        when {
            loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            notFound || user == null -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.userNotFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> {
                val u = user!!
                val avatarFull = u.avatarUrl?.let { base.trimEnd('/') + it }
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Foto grande — toca pra expandir (só se tiver foto).
                    Avatar(
                        seed = u.name ?: u.username,
                        size = 120.dp,
                        avatarPath = u.avatarUrl,
                        onClick = { if (avatarFull != null) expand = true },
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        u.name?.takeIf { it.isNotBlank() } ?: u.username,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "@${u.username}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(
                            text = s.profileMessage,
                            onClick = { onOpenChat(u.id, u.username) },
                        )
                        // Adicionar OU Excluir, conforme já ser contato salvo.
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    feedback = null
                                    try {
                                        if (isSaved) {
                                            repo.removeContact(u.id)
                                            isSaved = false
                                        } else {
                                            repo.addContact(u.username)
                                            isSaved = true
                                        }
                                    } catch (e: Exception) {
                                        feedback = e.message
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        ) {
                            Text(if (isSaved) "Excluir contato" else s.profileAddFriend)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Bloquear/Desbloquear + Reportar.
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    feedback = null
                                    try {
                                        if (isBlockedState) {
                                            repo.unblockUser(u.id); isBlockedState = false
                                        } else {
                                            repo.blockUser(u.id); isBlockedState = true
                                        }
                                    } catch (e: Exception) {
                                        feedback = e.message
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(if (isBlockedState) "Desbloquear" else "Bloquear")
                        }
                        TextButton(onClick = { showReport = true }) { Text("Reportar") }
                    }

                    if (feedback != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(feedback!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Diálogo de denúncia.
    if (showReport) {
        val u = user
        ReportDialog(
            onDismiss = { showReport = false },
            onSend = { reason ->
                if (u != null) {
                    scope.launch {
                        try {
                            repo.reportUser(u.id, reason)
                            feedback = "Denúncia enviada."
                        } catch (e: Exception) {
                            feedback = e.message
                        }
                        showReport = false
                    }
                }
            },
        )
    }

    if (expand) {
        val u = user
        val avatarFull = u?.avatarUrl?.let { base.trimEnd('/') + it }
        FullscreenImage(url = avatarFull, onDismiss = { expand = false })
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar usuário") },
        text = {
            Column {
                Text("Conte o que aconteceu. A denúncia vai para o suporte do app.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = { onSend(reason.trim()) }) {
                Text("Enviar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
