package com.privatemessenger.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.ui.theme.ChatWallpaper
import com.privatemessenger.app.ui.theme.FontSize
import com.privatemessenger.app.ui.theme.ThemeMode
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
        // Preferências de aparência/idioma. Ficam FORA do clear() de logout: são
        // do aparelho, não da conta (não faz sentido perder o tema ao deslogar).
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val LANG = stringPreferencesKey("lang")
        val WALLPAPER = stringPreferencesKey("wallpaper")
    }

    // Flows: valores que "avisam" a interface quando mudam. Ex: ao deslogar, as
    // telas que observam o token reagem sozinhas.
    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val userId: Flow<Long?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val baseUrl: Flow<String> = context.dataStore.data.map {
        // Padrão: IP do servidor Go na rede local (Wi-Fi de casa). Se o emulador
        // e o servidor estivessem na MESMA máquina, usaríamos 10.0.2.2 (é como o
        // emulador enxerga o "localhost" do PC). Como aqui o servidor roda em
        // outra máquina (Ubuntu), usamos o IP dela na rede. Se rodar em celular
        // físico fora de casa, troque pela URL do Cloudflare Tunnel.
        it[Keys.BASE_URL] ?: "http://192.168.1.225:8080"
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

    // --- Preferências (aparência + idioma) ---

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME] ?: "") }.getOrDefault(ThemeMode.ESCURO)
    }
    val fontSize: Flow<FontSize> = context.dataStore.data.map {
        runCatching { FontSize.valueOf(it[Keys.FONT] ?: "") }.getOrDefault(FontSize.NORMAL)
    }
    val language: Flow<AppLanguage> = context.dataStore.data.map {
        runCatching { AppLanguage.valueOf(it[Keys.LANG] ?: "") }.getOrDefault(AppLanguage.PORTUGUES)
    }
    val chatWallpaper: Flow<ChatWallpaper> = context.dataStore.data.map {
        runCatching { ChatWallpaper.valueOf(it[Keys.WALLPAPER] ?: "") }.getOrDefault(ChatWallpaper.PADRAO)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }
    suspend fun setFontSize(size: FontSize) {
        context.dataStore.edit { it[Keys.FONT] = size.name }
    }
    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.LANG] = lang.name }
    }
    suspend fun setChatWallpaper(w: ChatWallpaper) {
        context.dataStore.edit { it[Keys.WALLPAPER] = w.name }
    }

    // clear() remove só a SESSÃO (token + id). Mantém a URL do servidor e as
    // preferências de aparência/idioma, que são do aparelho e não da conta.
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USER_ID)
        }
    }

    // Leituras "uma vez" (sem observar), úteis dentro do cliente de rede.
    suspend fun currentToken(): String? = token.first()
    suspend fun currentBaseUrl(): String = baseUrl.first()
    suspend fun currentUserId(): Long? = userId.first()
}
