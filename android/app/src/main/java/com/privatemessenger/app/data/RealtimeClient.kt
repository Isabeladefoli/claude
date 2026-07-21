package com.privatemessenger.app.data

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// RealtimeClient mantém a conexão WebSocket com o servidor e "emite" as
// mensagens que chegam em tempo real.
//
// Como funciona pra quem usa: a interface se inscreve no fluxo `incoming` e,
// toda vez que o servidor empurra uma mensagem, ela aparece ali na hora.
//
// Também tratamos reconexão: se a internet cair, esperamos um pouco e tentamos
// conectar de novo. Enquanto isso, o app continua funcionando via HTTP (mandar
// e carregar histórico não dependem do WebSocket).
// ---------------------------------------------------------------------------
class RealtimeClient(
    private val api: ApiClient,
    private val tokenStore: TokenStore,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // SharedFlow é um "canal" de eventos. Quem se inscrever recebe as mensagens
    // conforme elas chegam.
    private val _incoming = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Message> = _incoming

    private var job: Job? = null

    // start abre a conexão e fica ouvindo (com reconexão automática).
    fun start() {
        if (job?.isActive == true) return // já está rodando
        job = scope.launch {
            while (isActive) {
                try {
                    connectOnce()
                } catch (_: Exception) {
                    // Caiu? Espera 3s e tenta de novo.
                }
                if (isActive) delay(3000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    // connectOnce abre UMA conexão e lê frames até ela cair.
    private suspend fun connectOnce() {
        val token = tokenStore.currentToken() ?: return
        val base = tokenStore.currentBaseUrl()

        // http:// vira ws://  e  https:// vira wss://  (protocolo do WebSocket).
        val wsBase = base
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .trimEnd('/')

        api.http.webSocket("$wsBase/ws?token=$token") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    handleFrame(frame.readText())
                }
            }
        }
    }

    // handleFrame decodifica o envelope e, se for uma mensagem, emite pro app.
    private suspend fun handleFrame(text: String) {
        val envelope = runCatching { json.decodeFromString<WsEnvelope>(text) }.getOrNull() ?: return
        when (envelope.type) {
            "message", "group_message" -> {
                val msg = runCatching { json.decodeFromJsonElement(Message.serializer(), envelope.data) }
                    .getOrNull() ?: return
                _incoming.emit(msg)
            }
            // outros tipos (ex: "typing") podem ser tratados aqui no futuro.
        }
    }
}
