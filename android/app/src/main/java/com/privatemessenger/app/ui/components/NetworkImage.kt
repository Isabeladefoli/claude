package com.privatemessenger.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// ---------------------------------------------------------------------------
// Carregador de imagem simples, SEM biblioteca externa (pra não depender de
// baixar nada novo no Gradle). Baixa os bytes da URL numa thread de fundo e
// decodifica pra uma imagem que o Compose sabe desenhar.
//
// Cache em memória: guardamos as fotos já baixadas por URL. Assim, quando você
// sai de uma tela e volta (ex: entra nas configurações e volta pros chats), a
// foto aparece NA HORA, sem baixar de novo — antes ela "sumia" por 1-2s porque
// recarregava toda vez.
// ---------------------------------------------------------------------------

// Guarda até ~100 imagens. Suficiente pra lista de conversas + perfis + fotos
// no chat, sem a foto "sumir" quando você navega bastante pelo app.
private val imageCache = LruCache<String, ImageBitmap>(100)

@Composable
fun rememberNetworkImage(url: String?): ImageBitmap? {
    // Começa já com a versão do cache (se houver) — é isso que mata o flicker.
    var bitmap by remember(url) { mutableStateOf(url?.let { imageCache.get(it) }) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        // Já está no cache? Usa e pronto (não baixa de novo).
        val cached = imageCache.get(url)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                conn.inputStream.use { input ->
                    val bytes = input.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null // falhou (rede/foto) -> quem chama mostra a inicial
            }
        }
        if (loaded != null) imageCache.put(url, loaded)
        bitmap = loaded
    }
    return bitmap
}
