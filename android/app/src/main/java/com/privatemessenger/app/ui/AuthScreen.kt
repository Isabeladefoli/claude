package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.privatemessenger.app.data.MessengerRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.vm.AuthViewModel

// Tela de login / cadastro. SEM design por enquanto: campos e botões padrão.
// Um botão embaixo alterna entre "Entrar" e "Criar conta".
@Composable
fun AuthScreen(repo: MessengerRepository) {
    val vm: AuthViewModel = viewModel(factory = authViewModelFactory(repo))
    val state by vm.state.collectAsState()

    // Estado local dos campos de texto (fica só nesta tela).
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (isRegister) "Criar conta" else "Entrar")

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nome de usuário") },
            singleLine = true,
            modifier = Modifier.padding(top = 16.dp),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { vm.submit(isRegister, username, password) },
            enabled = !state.loading,
            modifier = Modifier.padding(top = 16.dp),
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
    }
}
