package com.privatemessenger.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.TopBar
import kotlinx.coroutines.launch

// Idiomas: bandeira + nome. Toca pra escolher; o app muda de idioma na hora.
@Composable
fun LanguagesScreen(repo: MessengerRepository, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val current by repo.language.collectAsState(initial = AppLanguage.INGLES)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = s.languages, onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text(s.chooseLanguage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        // Ordem: norueguês, português, inglês, espanhol (como você listou).
        val langs = listOf(
            AppLanguage.NORUEGUES,
            AppLanguage.PORTUGUES,
            AppLanguage.INGLES,
            AppLanguage.ESPANHOL,
        )
        langs.forEach { lang ->
            LanguageRow(
                lang = lang,
                selected = lang == current,
                onClick = { scope.launch { repo.setLanguage(lang) } },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LanguageRow(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(lang.flag, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Text(
                lang.label,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
