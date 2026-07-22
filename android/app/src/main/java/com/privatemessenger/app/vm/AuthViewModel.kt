package com.privatemessenger.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privatemessenger.app.data.ApiException
import com.privatemessenger.app.data.DeviceKeys
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Um ViewModel guarda o estado de uma tela e a lógica de reagir às ações do
// usuário, SEM saber desenhar nada. A tela (Compose) observa o estado e se
// redesenha quando ele muda. Isso separa "o que acontece" de "como aparece".
// ---------------------------------------------------------------------------

// Estado da tela de login/cadastro.
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val repo: MessengerRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // submit trata tanto login quanto cadastro (isRegister decide qual).
    // No cadastro, name e birthday são opcionais (a pessoa preenche se quiser).
    fun submit(
        isRegister: Boolean,
        username: String,
        password: String,
        name: String = "",
        birthday: String = "",
        avatarBytes: ByteArray? = null,
        avatarMime: String? = null,
    ) {
        // Validação simples no cliente, antes de bater no servidor.
        if (username.isBlank() || password.length < 8) {
            _state.value = AuthUiState(error = "Usuário e senha (mín. 8 caracteres) são obrigatórios")
            return
        }

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                if (isRegister) {
                    val publicKey = DeviceKeys.placeholderPublicKey()
                    repo.register(
                        username.trim(),
                        password,
                        publicKey,
                        name = name.trim().ifBlank { null },
                        birthday = birthday.trim().ifBlank { null },
                    )
                    // Se a pessoa escolheu foto no cadastro, sobe agora (já está
                    // logada). Se falhar, não atrapalha a criação da conta.
                    if (avatarBytes != null) {
                        try {
                            val path = repo.uploadMedia(avatarBytes, "avatar", avatarMime ?: "image/jpeg")
                            repo.updateProfile(avatarUrl = path)
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    repo.login(username.trim(), password)
                }
                // Sucesso: o app reage sozinho ao token que apareceu no fluxo de
                // sessão; aqui só limpamos o estado de carregamento.
                _state.value = AuthUiState()
            } catch (e: ApiException) {
                _state.value = AuthUiState(error = e.message)
            } catch (e: Exception) {
                _state.value = AuthUiState(error = "Falha de conexão: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
