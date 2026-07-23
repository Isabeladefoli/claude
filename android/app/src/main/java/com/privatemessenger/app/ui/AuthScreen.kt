package com.privatemessenger.app.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.vm.AuthViewModel
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
    val s = LocalStrings.current

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
                        if (isRegister) s.signupSubtitle else s.loginSubtitle,
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
                            s.choosePhoto,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Usuário (ícone de pessoa à direita).
                    OutlinedTextField(
                        value = username,
                        onValueChange = { if (it.length <= 30) username = it },
                        placeholder = { Text(s.fieldUsername) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))

                    // Senha (ícone de olho pra mostrar/ocultar).
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(s.passwordHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showPass) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPass = !showPass }) {
                                Text(if (showPass) s.hide else s.show, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Campos extras só no cadastro: nome + aniversário (seletor).
                    if (isRegister) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 30) name = it },
                            placeholder = { Text(s.nameHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        // Aniversário escrito à mão: as barras aparecem sozinhas
                        // conforme você digita os números (dd/mm/aaaa).
                        OutlinedTextField(
                            value = birthday,
                            onValueChange = { birthday = formatBirthday(it) },
                            placeholder = { Text("${s.fieldBirthday} (dd/mm/aaaa)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        // "Forgot password?" só no login (por ora, só visual).
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { /* recuperação de senha: futuro */ }) {
                                Text(s.forgotPassword, fontSize = 13.sp)
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
                        Text(if (isRegister) s.signupBtn else s.loginBtn)
                    }

                    TextButton(onClick = { isRegister = !isRegister; vm.clearError() }) {
                        Text(if (isRegister) s.haveAccount else s.noAccount)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Versão — tocar aqui revela o campo do servidor (escondido).
            Text(
                s.version,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showServer = !showServer },
            )

            if (showServer) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; serverSaved = false },
                    label = { Text(s.serverAddress) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = {
                    scope.launch { repo.setBaseUrl(serverUrl.trim()); serverSaved = true }
                }) {
                    Text(if (serverSaved) s.addressSaved else s.saveAddress)
                }
            }
        }
    }
}

// Formata o aniversário conforme a pessoa digita: só números, com barras
// automáticas -> "11/10/2008". Máximo 8 dígitos (dd mm aaaa).
private fun formatBirthday(input: String): String {
    val digits = input.filter { it.isDigit() }.take(8)
    val sb = StringBuilder()
    for (i in digits.indices) {
        if (i == 2 || i == 4) sb.append('/')
        sb.append(digits[i])
    }
    return sb.toString()
}
