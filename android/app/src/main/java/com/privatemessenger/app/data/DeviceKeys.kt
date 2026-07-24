package com.privatemessenger.app.data

import android.content.Context
import com.privatemessenger.app.crypto.CryptoUtils

// DeviceKeys — acesso às chaves de criptografia do aparelho.
class DeviceKeys(private val keyStore: KeyStore) {

    // Retorna a chave pública do usuário (enviada no registro).
    suspend fun publicKey(): String {
        val pair = keyStore.getOrGenerateKeyPair()
        return pair.publicKeyPem
    }

    // Retorna a chave privada (usada pra descriptografar mensagens recebidas).
    suspend fun privateKey(): String? {
        return keyStore.getPrivateKey()
    }

    // Diz se este aparelho já tem um par de chaves salvo.
    suspend fun hasKeys(): Boolean = keyStore.hasKeyPair()

    companion object {
        // Factory pra criar a partir de um contexto.
        fun create(context: Context): DeviceKeys {
            return DeviceKeys(KeyStore(context))
        }
    }
}
