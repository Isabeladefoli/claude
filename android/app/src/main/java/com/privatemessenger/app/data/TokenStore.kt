package com.privatemessenger.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore é o jeito moderno do Android de guardar preferências pequenas em
// disco (substitui o antigo SharedPreferences). Guardamos aqui:
//   - o token de login (pra não pedir senha toda vez que abre o app)
//   - o id do usuário logado
//   - a URL do servidor (pra você poder trocar entre localhost e Cloudflare)
private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = longPreferencesKey("user_id")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    // Flows: valores que "avisam" a interface quando mudam. Ex: ao deslogar, as
    // telas que observam o token reagem sozinhas.
    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val userId: Flow<Long?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val baseUrl: Flow<String> = context.dataStore.data.map {
        // Padrão: 10.0.2.2 é como o EMULADOR do Android enxerga o "localhost" do
        // seu PC. Se rodar em celular físico, troque pela URL do Cloudflare.
        it[Keys.BASE_URL] ?: "http://10.0.2.2:8080"
    }

    suspend fun saveSession(token: String, userId: Long) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
        }
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.BASE_URL] = url }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // Leituras "uma vez" (sem observar), úteis dentro do cliente de rede.
    suspend fun currentToken(): String? = token.first()
    suspend fun currentBaseUrl(): String = baseUrl.first()
    suspend fun currentUserId(): Long? = userId.first()
}
