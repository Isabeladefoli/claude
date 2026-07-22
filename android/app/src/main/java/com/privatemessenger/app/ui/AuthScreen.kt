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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.AuthViewModel
import kotlinx.coroutines.launch

// Tela de login / cadastro. Um botão embaixo alterna entre "Entrar" e "Criar
// conta". No cadastro aparecem também Nome e Aniversário (opcionais).
@Composable
fun AuthScreen(repo: MessengerRepository) {
    val vm: AuthViewModel = viewModel(factory = authViewModelFactory(repo))
    val state by vm.state.collectAsState()

    val scope = rememberCoroutineScope()

    // Estado local dos campos de texto (fica só nesta tela).
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }

    // Endereço do servidor: carregamos o valor atual e deixamos editável, pra
    // você trocar o IP quando ele mudar SEM precisar recompilar o app.
    var serverUrl by remember { mutableStateOf("") }
    var serverSaved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { serverUrl = repo.currentBaseUrl() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (isRegister) "Signup" else "Login",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (isRegister) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Guarde seu usuário e senha pra entrar de novo depois.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nome de usuário") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        // Campos extras só no cadastro.
        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = birthday,
                onValueChange = { birthday = it },
                label = { Text("Aniversário (ex: 11/11/2000)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { vm.submit(isRegister, username, password, name, birthday) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(if (isRegister) "Cadastrar" else "Entrar")
        }

        TextButton(
            onClick = {
                isRegister = !isRegister
                vm.clearError()
            },
        ) {
            Text(
                if (isRegister) "Já tenho conta — Entrar"
                else "Não tenho conta — Criar",
            )
        }

        // --- Endereço do servidor (pra ajustar o IP sem recompilar) ---
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            "Endereço do servidor",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Se o app não conectar, ajuste aqui o IP do servidor (e toque em Salvar).",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it; serverSaved = false },
            label = { Text("Ex: http://192.168.1.225:8080") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        TextButton(
            onClick = {
                scope.launch {
                    repo.setBaseUrl(serverUrl.trim())
                    serverSaved = true
                }
            },
        ) {
            Text(if (serverSaved) "Salvo ✓" else "Salvar endereço")
        }
    }
}
