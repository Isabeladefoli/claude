package com.privatemessenger.app.data

import android.util.Base64
import java.security.SecureRandom

// ---------------------------------------------------------------------------
// DeviceKeys — as chaves de criptografia do aparelho.
//
// >>> PLACEHOLDER: por enquanto (fluxo primeiro) geramos só um valor aleatório
// pra ter algo no campo public_key que o servidor exige. <<<
//
// No passo E2E, isto vira a coisa de verdade:
//   1. Gerar um PAR de chaves X25519 (pública + privada) com libsodium.
//   2. Guardar a chave PRIVADA de forma segura no aparelho (Android Keystore),
//      pra ela NUNCA sair daqui.
//   3. Enviar só a chave PÚBLICA ao servidor no registro.
// ---------------------------------------------------------------------------
object DeviceKeys {

    // Gera um "public key" temporário (só aleatoriedade em base64).
    fun placeholderPublicKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
