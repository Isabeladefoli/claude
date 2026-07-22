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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privatemessenger.app.BUILD_TAG
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.MenuRow
import com.privatemessenger.app.ui.components.TopBar

// Menu de configurações (abre pela engrenagem da lista de conversas).
// Espelha o mockup: Account details / Preferences / Languages / Support /
// Version 1.0 (só mostra, não abre nada) / Log out (com confirmação).
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountDetails: () -> Unit,
    onPreferences: () -> Unit,
    onLanguages: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit,
) {
    val s = LocalStrings.current
    var confirmLogout by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = s.settings, onBack = onBack)
        Spacer(Modifier.height(8.dp))

        MenuRow(s.accountDetails, onAccountDetails)
        HorizontalDivider()
        MenuRow(s.preferences, onPreferences)
        HorizontalDivider()
        MenuRow(s.languages, onLanguages)
        HorizontalDivider()
        MenuRow(s.support, onSupport)
        HorizontalDivider()

        // Versão: linha "morta" de propósito (só informativa).
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text("${s.version}  ·  build $BUILD_TAG", fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { confirmLogout = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(s.logout)
        }
    }

    // Pop-up "tem certeza mesmo que quer sair?" — sim / não.
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(s.logoutConfirmTitle) },
            text = { Text(s.logoutConfirmMsg) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(s.yes) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(s.no) }
            },
        )
    }
}
