package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.PrimaryButton
import com.privatemessenger.app.ui.components.TopBar
import kotlinx.coroutines.launch

// Suporte: e-mail de resposta + título + texto do report. Ao enviar, guardamos
// no servidor (vai pro e-mail de suporte da dona do app). Aviso deixa isso claro.
@Composable
fun SupportScreen(repo: MessengerRepository, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = s.support, onBack = onBack)
        Spacer(Modifier.height(8.dp))

        // Aviso obrigatório: pra onde vai o report.
        Text(s.supportNotice, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; feedback = null },
            label = { Text(s.supportEmailLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; feedback = null },
            label = { Text(s.supportSubjectLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it; feedback = null },
            label = { Text(s.supportBodyLabel) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
        )

        if (feedback != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                feedback!!,
                color = if (success) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = s.send,
            enabled = !sending,
            onClick = {
                scope.launch {
                    sending = true
                    feedback = null
                    try {
                        repo.sendSupport(email, title, body)
                        success = true
                        feedback = s.supportSent
                        email = ""; title = ""; body = ""
                    } catch (e: Exception) {
                        success = false
                        feedback = e.message
                    } finally {
                        sending = false
                    }
                }
            },
        )
    }
}
