package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.AuthViewModel

// Tela de login / cadastro. Um botão embaixo alterna entre "Entrar" e "Criar
// conta". No cadastro aparecem também Nome e Aniversário (opcionais).
@Composable
fun AuthScreen(repo: MessengerRepository) {
    val vm: AuthViewModel = viewModel(factory = authViewModelFactory(repo))
    val state by vm.state.collectAsState()

    // Estado local dos campos de texto (fica só nesta tela).
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
    }
}
