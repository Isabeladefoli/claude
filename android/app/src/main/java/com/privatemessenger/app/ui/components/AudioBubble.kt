package com.privatemessenger.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Bolha de áudio: um botão redondo de tocar/parar + o texto "Áudio". Usa o
// MediaPlayer do Android pra tocar direto da URL (streaming). Tudo protegido por
// try/catch pra não derrubar o app se o arquivo falhar.
@Composable
fun AudioBubble(url: String, contentColor: Color, buttonBg: Color) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }

    // Ao sair da tela, solta o player (libera memória/áudio).
    DisposableEffect(url) {
        onDispose {
            runCatching { player?.release() }
            player = null
        }
    }

    fun stop() {
        runCatching { player?.release() }
        player = null
        playing = false
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .clickable {
                    if (playing) {
                        stop()
                    } else {
                        try {
                            val mp = MediaPlayer()
                            mp.setDataSource(url)
                            mp.setOnPreparedListener { it.start(); playing = true }
                            mp.setOnCompletionListener { stop() }
                            mp.setOnErrorListener { _, _, _ -> stop(); true }
                            mp.prepareAsync()
                            player = mp
                        } catch (_: Exception) {
                            stop()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (playing) "||" else ">", color = contentColor, fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text("Áudio", color = contentColor)
    }
}
