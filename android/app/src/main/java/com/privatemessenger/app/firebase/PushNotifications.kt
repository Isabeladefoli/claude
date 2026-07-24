package com.privatemessenger.app.firebase

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.privatemessenger.app.data.MessengerRepository
import kotlinx.coroutines.tasks.await

// Funções pra gerenciar push notifications: obter token, registrar no servidor.
object PushNotifications {

    // Obtém o device token do Firebase e o registra no servidor.
    // Chamado quando o app faz login ou ao abrir se já tá logado.
    suspend fun registerDeviceToken(context: Context, repo: MessengerRepository) {
        try {
            // Tenta obter do cache local primeiro (mais rápido).
            val prefs = context.getSharedPreferences("fcm", Context.MODE_PRIVATE)
            val cachedToken = prefs.getString("device_token", null)

            val token = cachedToken ?: obtainToken()
            if (token != null) {
                // Envia pro servidor.
                repo.registerDeviceToken(token)
            }
        } catch (e: Exception) {
            // Silencioso: falha em notificação não deve derrubar o login.
            e.printStackTrace()
        }
    }

    // Obtém um novo token do Firebase.
    private suspend fun obtainToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }
}
