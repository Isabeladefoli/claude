package com.privatemessenger.app.ui.components

import android.graphics.Bitmap
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
// Carregador de imagem simples, SEM biblioteca externa.
//
// DUAS coisas importantes pra performance (senão o app trava):
//  1) REDUZIR A RESOLUÇÃO ao decodificar. Uma foto de celular tem milhões de
//     pixels; mostrada num avatar de 44dp, não precisa de tudo isso. Carregar
//     em resolução cheia estoura a memória (foi o que deixou o app a 4fps).
//     Por isso passamos "maxPx": o maior lado da imagem decodificada.
//  2) CACHE LIMITADO POR MEMÓRIA (bytes), não por quantidade. Assim guardamos
//     muitas imagens pequenas OU poucas grandes, sem nunca passar do teto.
// ---------------------------------------------------------------------------

// Teto de ~24 MB de imagens em memória. sizeOf mede cada imagem (w*h*4 bytes).
private val imageCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

// Decodifica os bytes JÁ reduzindo pra no máximo ~maxPx no maior lado.
private fun decodeSampled(bytes: ByteArray, maxPx: Int): Bitmap? {
    // 1ª passada: só mede o tamanho, sem carregar os pixels.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val larger = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)

    // Descobre o fator de redução (potência de 2) pra caber em maxPx.
    var sample = 1
    while (larger / sample > maxPx) sample *= 2

    // 2ª passada: carrega já reduzida.
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

@Composable
fun rememberNetworkImage(url: String?, maxPx: Int = 512): ImageBitmap? {
    val key = if (url == null) null else "$url@$maxPx"
    var bitmap by remember(key) { mutableStateOf(key?.let { imageCache.get(it) }) }

    LaunchedEffect(key) {
        if (key == null || url == null) {
            bitmap = null
            return@LaunchedEffect
        }
        val cached = imageCache.get(key)
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
                    decodeSampled(bytes, maxPx)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
        if (loaded != null) imageCache.put(key, loaded)
        bitmap = loaded
    }
    return bitmap
}
