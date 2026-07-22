package com.privatemessenger.app.ui.components

import android.graphics.BitmapFactory
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
// baixar nada novo no Gradle). Baixa os bytes da URL numa thread de fundo,
// decodifica pra uma imagem que o Compose sabe desenhar, e guarda o resultado
// por URL — então cada foto é baixada uma vez só.
//
// É básico de propósito: sem cache em disco, sem redimensionar. Pra fotos
// pequenas na rede local, dá conta. Se um dia quisermos algo mais parrudo,
// aí sim vale uma lib como a Coil.
// ---------------------------------------------------------------------------
@Composable
fun rememberNetworkImage(url: String?): ImageBitmap? {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
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
    }
    return bitmap
}
