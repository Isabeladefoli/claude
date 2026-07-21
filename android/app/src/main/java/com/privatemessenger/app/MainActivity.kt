package com.privatemessenger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.ui.AuthScreen
import com.privatemessenger.app.ui.ChatScreen
import com.privatemessenger.app.ui.ConversationsScreen
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// MainActivity é o ponto de entrada do app Android. Ela:
//   1. Cria o repositório (a camada de dados).
//   2. Decide qual tela mostrar com base na sessão:
//        - sem token  -> tela de login/cadastro
//        - com token   -> lista de conversas (ou uma conversa aberta)
//
// A navegação aqui é simples de propósito (um estado que diz "qual tela"), sem
// biblioteca extra. Dá pra trocar por Navigation Compose quando o app crescer.
// ---------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = MessengerRepository(applicationContext)
        // Se já havia sessão salva, religa o tempo real ao abrir o app.
        repo.resumeRealtimeIfLoggedIn()

        setContent {
            // Usamos o tema padrão do Material3 (sem cores customizadas por ora).
            MaterialTheme {
                Surface {
                    AppRoot(repo)
                }
            }
        }
    }
}

// Representa qual tela está aberta dentro da área logada.
private sealed interface Screen {
    data object Conversations : Screen
    data class Chat(val partnerId: Long, val partnerUsername: String) : Screen
}

@Composable
private fun AppRoot(repo: MessengerRepository) {
    // Observa o token: se for null, mostramos login; se existir, a área logada.
    val token by repo.token.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    if (token == null) {
        AuthScreen(repo)
        return
    }

    // Área logada: controla qual tela interna está aberta.
    var screen by remember { mutableStateOf<Screen>(Screen.Conversations) }

    when (val s = screen) {
        is Screen.Conversations -> ConversationsScreen(
            repo = repo,
            onOpenChat = { id, name -> screen = Screen.Chat(id, name) },
            onLogout = {
                scope.launch { repo.logout() }
                screen = Screen.Conversations // reseta pra próxima sessão
            },
        )

        is Screen.Chat -> ChatScreen(
            repo = repo,
            partnerId = s.partnerId,
            partnerUsername = s.partnerUsername,
            onBack = { screen = Screen.Conversations },
        )
    }
}
