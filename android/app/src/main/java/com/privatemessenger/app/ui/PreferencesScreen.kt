package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.TopBar
import com.privatemessenger.app.ui.theme.ChatWallpaper
import com.privatemessenger.app.ui.theme.FontSize
import com.privatemessenger.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

// Preferências: modo escuro/claro e tamanho da letra. Tudo salvo na hora e
// aplicado no app inteiro (a MainActivity observa e re-tematiza sozinha).
@Composable
fun PreferencesScreen(repo: MessengerRepository, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val theme by repo.themeMode.collectAsState(initial = ThemeMode.ESCURO)
    val font by repo.fontSize.collectAsState(initial = FontSize.NORMAL)
    val wallpaper by repo.chatWallpaper.collectAsState(initial = ChatWallpaper.PADRAO)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar(title = s.preferences, onBack = onBack)
        Spacer(Modifier.height(16.dp))

        // --- Aparência (tema) ---
        Text(s.appearance, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(s.darkMode, selected = theme == ThemeMode.ESCURO) {
                scope.launch { repo.setThemeMode(ThemeMode.ESCURO) }
            }
            Choice(s.lightMode, selected = theme == ThemeMode.CLARO) {
                scope.launch { repo.setThemeMode(ThemeMode.CLARO) }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Tamanho da letra ---
        Text(s.textSize, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(s.small, selected = font == FontSize.PEQUENA) {
                scope.launch { repo.setFontSize(FontSize.PEQUENA) }
            }
            Choice(s.normal, selected = font == FontSize.NORMAL) {
                scope.launch { repo.setFontSize(FontSize.NORMAL) }
            }
            Choice(s.large, selected = font == FontSize.GRANDE) {
                scope.launch { repo.setFontSize(FontSize.GRANDE) }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Papel de parede do chat ---
        Text("Papel de parede do chat", fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        // As opções em linhas de 3.
        ChatWallpaper.values().toList().chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { w ->
                    Choice(w.label, selected = wallpaper == w) {
                        scope.launch { repo.setChatWallpaper(w) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        // Uma amostra de texto pra a pessoa ver o efeito na hora.
        Text(
            "Aa — 💙",
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Um "botão de escolha": preenchido (azul) quando selecionado, contornado quando não.
@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
