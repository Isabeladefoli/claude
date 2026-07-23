package com.privatemessenger.app.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

// Grava áudio do microfone num arquivo temporário e devolve os bytes ao parar.
// Tudo protegido por try/catch: se algo falhar, devolve false/null em vez de
// derrubar o app. O arquivo temporário é apagado depois de lido.
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): Boolean {
        return try {
            val f = File.createTempFile("rec", ".m4a", context.cacheDir)
            // A partir do Android 12 (S) o construtor pede o context.
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(f.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            file = f
            true
        } catch (e: Exception) {
            cleanup()
            false
        }
    }

    // Para a gravação e devolve os bytes do áudio (ou null se falhou).
    fun stop(): ByteArray? {
        return try {
            recorder?.stop()
            val bytes = file?.readBytes()
            cleanup()
            bytes
        } catch (e: Exception) {
            cleanup()
            null
        }
    }

    fun cancel() = cleanup()

    private fun cleanup() {
        runCatching { recorder?.release() }
        recorder = null
        runCatching { file?.delete() }
        file = null
    }
}
