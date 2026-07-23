package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.data.User
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.ui.components.PrimaryButton
import com.privatemessenger.app.ui.components.TopBar
import kotlinx.coroutines.launch

// Página de perfil de um usuário pesquisado: foto (por ora, inicial), nome de
// exibição, @usuário e dois botões — Mensagem e Adicionar.
@Composable
fun ProfileScreen(
    repo: MessengerRepository,
    username: String,
    onBack: () -> Unit,
    onOpenChat: (userId: Long, username: String) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }
    var added by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    // Carrega o perfil ao abrir (uma vez por username).
    LaunchedEffect(username) {
        loading = true
        notFound = false
        try {
            user = repo.findUser(username)
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
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(seed = u.name ?: u.username, size = 96.dp, avatarPath = u.avatarUrl)
                    Spacer(Modifier.height(14.dp))
                    // Nome de exibição (se tiver) em destaque; @usuário embaixo.
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
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        repo.addContact(u.username)
                                        added = true
                                        addError = null
                                    } catch (e: Exception) {
                                        addError = e.message
                                    }
                                }
                            },
                            enabled = !added,
                        ) {
                            Text(if (added) "✓ ${s.profileAddFriend}" else s.profileAddFriend)
                        }
                    }
                    if (addError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(addError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
