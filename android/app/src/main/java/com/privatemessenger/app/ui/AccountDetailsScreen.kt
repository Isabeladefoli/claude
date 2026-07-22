package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.Avatar
import com.privatemessenger.app.ui.components.PrimaryButton
import com.privatemessenger.app.ui.components.TopBar
import kotlinx.coroutines.launch

// Account details: PRIMEIRO pede a senha (trava). Só depois mostra e deixa
// editar os dados, trocar senha e apagar a conta. Pedir a senha de novo é a
// confirmação de que é a dona da conta mesmo — o token não basta.
@Composable
fun AccountDetailsScreen(repo: MessengerRepository, onBack: () -> Unit) {
    var unlocked by remember { mutableStateOf(false) }

    if (!unlocked) {
        PasswordGate(repo, onBack) { unlocked = true }
    } else {
        AccountDetailsBody(repo, onBack)
    }
}

// Tela de trava: só a senha.
@Composable
private fun PasswordGate(repo: MessengerRepository, onBack: () -> Unit, onUnlocked: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(title = s.accountDetails, onBack = onBack)
        Spacer(Modifier.height(16.dp))
        Text(s.unlockMsg, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text(s.enterPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = s.unlock,
            enabled = !checking && password.isNotBlank(),
            onClick = {
                scope.launch {
                    checking = true
                    error = null
                    try {
                        if (repo.verifyPassword(password)) onUnlocked()
                        else error = s.wrongPassword
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        checking = false
                    }
                }
            },
        )
    }
}

// Corpo real: dados editáveis + trocar senha + apagar conta.
@Composable
private fun AccountDetailsBody(repo: MessengerRepository, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackOk by remember { mutableStateOf(true) }

    // Carrega o perfil uma vez ao abrir (LaunchedEffect roda só na entrada).
    LaunchedEffect(Unit) {
        try {
            val me = repo.me()
            username = me.username
            name = me.name ?: ""
            birthday = me.birthday ?: ""
        } catch (_: Exception) {
        }
    }

    // Trocar senha (campos e feedback próprios).
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var passFeedback by remember { mutableStateOf<String?>(null) }
    var passOk by remember { mutableStateOf(true) }

    // Apagar conta.
    var showDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TopBar(title = s.accountDetails, onBack = onBack)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(seed = name.ifBlank { username }, size = 84.dp)
        }
        Spacer(Modifier.height(20.dp))

        // Campos editáveis.
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; feedback = null },
            label = { Text(s.fieldUsername) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; feedback = null },
            label = { Text(s.fieldName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = birthday,
            onValueChange = { birthday = it; feedback = null },
            label = { Text(s.fieldBirthday) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (feedback != null) {
            Spacer(Modifier.height(8.dp))
            Text(feedback!!, color = if (feedbackOk) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = s.save,
            onClick = {
                scope.launch {
                    try {
                        repo.updateProfile(
                            username = username.trim(),
                            name = name,
                            birthday = birthday,
                        )
                        feedbackOk = true
                        feedback = s.saved
                    } catch (e: Exception) {
                        feedbackOk = false
                        feedback = e.message
                    }
                }
            },
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // --- Trocar senha ---
        Text(s.changePassword, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = currentPass,
            onValueChange = { currentPass = it; passFeedback = null },
            label = { Text(s.currentPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it; passFeedback = null },
            label = { Text(s.newPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (passFeedback != null) {
            Spacer(Modifier.height(8.dp))
            Text(passFeedback!!, color = if (passOk) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = s.changePassword,
            enabled = currentPass.isNotBlank() && newPass.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        repo.changePassword(currentPass, newPass)
                        passOk = true
                        passFeedback = s.passwordChanged
                        currentPass = ""; newPass = ""
                    } catch (e: Exception) {
                        passOk = false
                        passFeedback = e.message
                    }
                }
            },
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // --- Apagar conta (perigo) ---
        OutlinedButton(
            onClick = { showDelete = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(s.deleteAccount)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDelete) {
        DeleteAccountDialog(
            repo = repo,
            onDismiss = { showDelete = false },
        )
    }
}

// Pop-up de apagar conta: pede a senha de novo (irreversível). Ao apagar, a
// sessão é limpa no repositório e o app volta sozinho pro login.
@Composable
private fun DeleteAccountDialog(repo: MessengerRepository, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text(s.deleteAccount) },
        text = {
            Column {
                Text(s.deleteAccountConfirm)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(s.enterPassword) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        deleting = true
                        error = null
                        try {
                            repo.deleteAccount(password)
                            // Sessão limpa -> AppRoot volta pro login sozinho.
                        } catch (e: Exception) {
                            error = e.message
                            deleting = false
                        }
                    }
                },
                enabled = !deleting && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(s.delete) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) { Text(s.cancel) }
        },
    )
}
