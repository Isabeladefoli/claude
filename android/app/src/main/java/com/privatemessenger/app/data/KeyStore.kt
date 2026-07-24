package com.privatemessenger.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.privatemessenger.app.crypto.CryptoUtils

private val Context.dataStore by preferencesDataStore(name = "keys")

class KeyStore(private val context: Context) {
    companion object {
        private val PUBLIC_KEY = stringPreferencesKey("public_key")
        private val PRIVATE_KEY = stringPreferencesKey("private_key")
    }

    // Retorna o par de chaves do usuário local, ou gera um novo.
    suspend fun getOrGenerateKeyPair(): CryptoUtils.KeyPair {
        val prefs = context.dataStore.data.first()
        val pub = prefs[PUBLIC_KEY]
        val priv = prefs[PRIVATE_KEY]

        if (pub != null && priv != null) {
            return CryptoUtils.KeyPair(publicKeyPem = pub, privateKeyPem = priv)
        }

        // Gera um novo par e salva.
        val newPair = CryptoUtils.generateKeyPair()
        context.dataStore.edit { prefs ->
            prefs[PUBLIC_KEY] = newPair.publicKeyPem
            prefs[PRIVATE_KEY] = newPair.privateKeyPem
        }
        return newPair
    }

    // Retorna a chave privada do usuário local (necessária pra descriptografar).
    suspend fun getPrivateKey(): String? {
        return context.dataStore.data.map { it[PRIVATE_KEY] }.first()
    }

    // Fluxo reativo da chave pública (útil pra UI, se necessário).
    fun publicKeyFlow() = context.dataStore.data.map { it[PUBLIC_KEY] }
}
