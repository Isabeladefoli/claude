package com.privatemessenger.app.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.AuthViewModel
import java.util.Calendar
import kotlinx.coroutines.launch

// Tela de login / cadastro, no estilo do mockup: um cartão central com título,
// campos com ícones e a opção de ver a senha. No cadastro tem foto de perfil,
// nome e aniversário (com seletor de data).
@Composable
fun AuthScreen(repo: MessengerRepository) {
    val vm: AuthViewModel = viewModel(factory = authViewModelFactory(repo))
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }

    // Foto escolhida no cadastro (bytes + tipo + prévia pra mostrar na tela).
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var avatarMime by remember { mutableStateOf<String?>(null) }
    var avatarPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            avatarBytes = bytes
            avatarMime = context.contentResolver.getType(uri) ?: "image/jpeg"
            avatarPreview = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
    }

    // Endereço do servidor: escondido; aparece ao tocar em "version 1.0".
    var showServer by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var serverSaved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { serverUrl = repo.currentBaseUrl() }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (isRegister) "Signup" else "Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isRegister) "Guarde seu usuário e senha pra entrar de novo depois."
                        else "Bem-vinda de volta, entre na sua conta.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Foto de perfil (só no cadastro, logo no começo).
                    if (isRegister) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    pickPhoto.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val preview = avatarPreview
                            if (preview != null) {
                                Image(
                                    bitmap = preview,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text("+", fontSize = 34.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Escolher foto de perfil",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Usuário (ícone de pessoa à direita).
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("User") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))

                    // Senha (ícone de olho pra mostrar/ocultar).
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showPass) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPass = !showPass }) {
                                Text(if (showPass) "ocultar" else "ver", fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Campos extras só no cadastro: nome + aniversário (seletor).
                    if (isRegister) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        // Campo "falso" que abre o seletor de data ao tocar.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { openDatePicker(context, birthday) { birthday = it } }
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                        ) {
                            Text(
                                birthday.ifBlank { "Aniversário (toque para escolher)" },
                                color = if (birthday.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        // "Forgot password?" só no login (por ora, só visual).
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { /* recuperação de senha: futuro */ }) {
                                Text("Forgot password?", fontSize = 13.sp)
                            }
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            vm.submit(isRegister, username, password, name, birthday, avatarBytes, avatarMime)
                        },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp).padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(if (isRegister) "Signup" else "Log in")
                    }

                    TextButton(onClick = { isRegister = !isRegister; vm.clearError() }) {
                        Text(
                            if (isRegister) "Já tenho conta — Entrar"
                            else "Não tem conta? Cadastre-se",
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // "version 1.0" — tocar aqui revela o campo do servidor (escondido).
            Text(
                "version 1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showServer = !showServer },
            )

            if (showServer) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; serverSaved = false },
                    label = { Text("Endereço do servidor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = {
                    scope.launch { repo.setBaseUrl(serverUrl.trim()); serverSaved = true }
                }) {
                    Text(if (serverSaved) "Salvo ✓" else "Salvar endereço")
                }
            }
        }
    }
}

// Abre o seletor de data nativo (dia/mês/ano) e devolve "dd/mm/aaaa".
private fun openDatePicker(
    context: android.content.Context,
    current: String,
    onPicked: (String) -> Unit,
) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked("%02d/%02d/%04d".format(day, month + 1, year)) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}
