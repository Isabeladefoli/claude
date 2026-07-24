package com.privatemessenger.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.ui.AccountDetailsScreen
import com.privatemessenger.app.ui.AuthScreen
import com.privatemessenger.app.ui.ChatScreen
import com.privatemessenger.app.ui.ConversationsScreen
import com.privatemessenger.app.ui.LanguagesScreen
import com.privatemessenger.app.ui.PreferencesScreen
import com.privatemessenger.app.ui.ProfileScreen
import com.privatemessenger.app.ui.SearchScreen
import com.privatemessenger.app.ui.SettingsScreen
import com.privatemessenger.app.ui.SupportScreen
import com.privatemessenger.app.ui.components.LocalBaseUrl
import com.privatemessenger.app.ui.theme.AppTheme
import com.privatemessenger.app.ui.theme.FontSize
import com.privatemessenger.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// MainActivity é o ponto de entrada do app Android. Ela:
//   1. Cria o repositório (a camada de dados).
//   2. Aplica o TEMA escolhido (escuro/claro, tamanho de letra, idioma).
//   3. Decide qual tela mostrar com base na sessão e na navegação interna.
//
// A navegação aqui é simples de propósito (um estado que diz "qual tela"), sem
// biblioteca extra. Dá pra trocar por Navigation Compose quando o app crescer.
// ---------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    // Pedido de permissão de notificação (Android 13+). O resultado em si não
    // muda nada aqui: se negar, só não aparecem notificações.
    private val askNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignora */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = MessengerRepository(applicationContext)
        // Se já havia sessão salva, religa o tempo real ao abrir o app.
        repo.resumeRealtimeIfLoggedIn()

        // Pede permissão de notificação em aparelhos Android 13+ (antes disso a
        // permissão é concedida automaticamente pela declaração no manifest).
        requestNotificationPermissionIfNeeded()

        setContent {
            App(repo, onLogoutCleanup = { viewModelStore.clear() })
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// App aplica o tema (observando as preferências salvas) e coloca o conteúdo
// dentro de uma Surface com a cor de fundo do tema.
@Composable
private fun App(repo: MessengerRepository, onLogoutCleanup: () -> Unit) {
    val theme by repo.themeMode.collectAsState(initial = ThemeMode.ESCURO)
    val font by repo.fontSize.collectAsState(initial = FontSize.NORMAL)
    val language by repo.language.collectAsState(initial = AppLanguage.INGLES)
    val baseUrl by repo.baseUrl.collectAsState(initial = "")

    AppTheme(themeMode = theme, fontSize = font, language = language) {
        // Deixa a URL do servidor disponível pra qualquer Avatar montar o
        // endereço completo da foto.
        CompositionLocalProvider(LocalBaseUrl provides baseUrl) {
            // safeDrawingPadding empurra o conteúdo pra longe da barra de status
            // (edge-to-edge é o padrão a partir do Android 15).
            Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                AppRoot(repo, onLogoutCleanup)
            }
        }
    }
}

// Representa qual tela está aberta dentro da área logada.
private sealed interface Screen {
    data object Conversations : Screen
    data class Chat(val partnerId: Long, val partnerUsername: String) : Screen
    data object Settings : Screen
    data object AccountDetails : Screen
    data object Preferences : Screen
    data object Languages : Screen
    data object Support : Screen
    data object Search : Screen
    // `from` guarda de qual tela o perfil foi aberto, pra o "voltar" retornar
    // pro lugar certo (busca, conversas ou um chat).
    data class Profile(val username: String, val from: Screen) : Screen
}

@Composable
private fun AppRoot(repo: MessengerRepository, onLogoutCleanup: () -> Unit) {
    // Observa o token: se for null, mostramos login; se existir, a área logada.
    val token by repo.token.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    if (token == null) {
        AuthScreen(repo)
        return
    }

    // Área logada: controla qual tela interna está aberta.
    var screen by remember { mutableStateOf<Screen>(Screen.Conversations) }

    // Botão "voltar" do Android (a barrinha de baixo): em vez de FECHAR o app,
    // ele volta DENTRO do app pra tela anterior. Só na tela raiz (Conversas) é
    // que o voltar sai do app (comportamento normal do Android). Assim você não
    // cai mais pra fora do app sem querer.
    BackHandler(enabled = screen !is Screen.Conversations) {
        val cur = screen
        screen = when (cur) {
            is Screen.AccountDetails,
            is Screen.Preferences,
            is Screen.Languages,
            is Screen.Support -> Screen.Settings
            is Screen.Profile -> cur.from
            else -> Screen.Conversations
        }
    }

    when (val s = screen) {
        is Screen.Conversations -> ConversationsScreen(
            repo = repo,
            onOpenChat = { id, name -> screen = Screen.Chat(id, name) },
            onOpenSettings = { screen = Screen.Settings },
            onOpenSearch = { screen = Screen.Search },
            onOpenProfile = { name -> screen = Screen.Profile(name, Screen.Conversations) },
        )

        is Screen.Chat -> ChatScreen(
            repo = repo,
            partnerId = s.partnerId,
            partnerUsername = s.partnerUsername,
            onBack = { screen = Screen.Conversations },
            onOpenProfile = { name -> screen = Screen.Profile(name, s) },
        )

        is Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.Conversations },
            onAccountDetails = { screen = Screen.AccountDetails },
            onPreferences = { screen = Screen.Preferences },
            onLanguages = { screen = Screen.Languages },
            onSupport = { screen = Screen.Support },
            onLogout = {
                scope.launch {
                    repo.logout()
                    // Limpa TODOS os ViewModels guardados (conversas, chats
                    // abertos). Sem isso, os dados da conta anterior ficariam na
                    // memória e "vazariam" pra próxima conta — bug de segurança.
                    onLogoutCleanup()
                }
                screen = Screen.Conversations // reseta pra próxima sessão
            },
        )

        is Screen.AccountDetails -> AccountDetailsScreen(
            repo = repo,
            onBack = { screen = Screen.Settings },
        )

        is Screen.Preferences -> PreferencesScreen(
            repo = repo,
            onBack = { screen = Screen.Settings },
        )

        is Screen.Languages -> LanguagesScreen(
            repo = repo,
            onBack = { screen = Screen.Settings },
        )

        is Screen.Support -> SupportScreen(
            repo = repo,
            onBack = { screen = Screen.Settings },
        )

        is Screen.Search -> SearchScreen(
            onBack = { screen = Screen.Conversations },
            onSearch = { username -> screen = Screen.Profile(username, Screen.Search) },
        )

        is Screen.Profile -> ProfileScreen(
            repo = repo,
            username = s.username,
            onBack = { screen = s.from },
            onOpenChat = { id, name -> screen = Screen.Chat(id, name) },
        )
    }
}
