package com.privatemessenger.app.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.privatemessenger.app.R

// Serviço que recebe notificações push do Firebase Cloud Messaging.
// Chamado automaticamente quando uma mensagem chega (app em foreground ou background).
class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Novo token gerado (pode acontecer periodicamente ou quando o app reinstala).
        // Salvamos pra enviar pro servidor mais tarde quando o user faz login.
        saveTokenLocally(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Se a mensagem tiver dados custom, mostramos uma notificação.
        // FCM manda a notificação automaticamente em background, mas como estamos
        // em desenvolvimento, vamos criar manualmente em ambos os casos.
        val title = message.notification?.title ?: "Private Messenger"
        val body = message.notification?.body ?: "Nova mensagem"

        createNotification(title, body)
    }

    private fun createNotification(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Cria o canal de notificação (Android 8+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensagens",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificações de novas mensagens"
                enableVibration(true)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }

        // Constrói a notificação.
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // seu app icon aqui
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        // Mostra a notificação.
        manager.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun saveTokenLocally(token: String) {
        val prefs = getSharedPreferences("fcm", MODE_PRIVATE)
        prefs.edit().putString("device_token", token).apply()
    }

    companion object {
        private const val CHANNEL_ID = "messages"
    }
}
