package com.privatemessenger.app.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// E2E encryption utilities: gera chaves, faz ECDH, criptografa/descriptografa com AES-256-GCM.

object CryptoUtils {
    data class KeyPair(
        val publicKeyPem: String,  // base64 da chave pública codificada
        val privateKeyPem: String, // base64 da chave privada codificada
    )

    data class EncryptedMessage(
        val ciphertext: String, // base64
        val nonce: String,      // base64
    )

    private const val KEY_ALGORITHM = "EC"
    private const val CURVE_NAME = "prime256v1" // P-256, amplamente suportado
    private const val AGREEMENT_ALGORITHM = "ECDH"
    private const val CIPHER_ALGORITHM = "AES"
    private const val CIPHER_MODE = "GCM"
    private const val NONCE_SIZE_BITS = 96 // 12 bytes pro GCM
    private const val TAG_SIZE_BITS = 128   // 16 bytes tag de autenticação
    private const val KEY_SIZE_BITS = 256    // AES-256
    private const val HMAC_ALGORITHM = "HmacSHA256"

    // Gera um novo par de chaves EC (P-256).
    fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyGen.initialize(ECGenParameterSpec(CURVE_NAME))
        val pair = keyGen.generateKeyPair()

        val publicKeyBytes = pair.public.encoded
        val privateKeyBytes = pair.private.encoded

        return KeyPair(
            publicKeyPem = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP),
            privateKeyPem = Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP),
        )
    }

    // Criptografa uma mensagem. Usa ECDH pra derivar chave compartilhada com a chave
    // pública do destinatário, depois AES-256-GCM.
    fun encrypt(
        plaintext: String,
        recipientPublicKeyBase64: String,
        senderPrivateKeyBase64: String,
    ): EncryptedMessage {
        // Decodifica a chave pública do destinatário.
        val recipientPublicKeyBytes = Base64.decode(recipientPublicKeyBase64, Base64.NO_WRAP)
        val recipientPublicKey = KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(recipientPublicKeyBytes))

        // Decodifica a chave privada do remetente.
        val senderPrivateKeyBytes = Base64.decode(senderPrivateKeyBase64, Base64.NO_WRAP)
        val senderPrivateKey = KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(senderPrivateKeyBytes))

        // Usa ECDH pra derivar a chave compartilhada.
        val keyAgreement = KeyAgreement.getInstance(AGREEMENT_ALGORITHM)
        keyAgreement.init(senderPrivateKey)
        keyAgreement.doPhase(recipientPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Deriva a chave AES-256 via HKDF (pseudo-HKDF com HMAC-SHA256).
        val derivedKey = hkdf(sharedSecret, KEY_SIZE_BITS / 8)

        // Gera um nonce aleatório (12 bytes pra GCM).
        val nonce = ByteArray(NONCE_SIZE_BITS / 8)
        SecureRandom().nextBytes(nonce)

        // Criptografa com AES-256-GCM.
        val cipher = Cipher.getInstance("$CIPHER_ALGORITHM/$CIPHER_MODE/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, nonce)
        val secretKey = SecretKeySpec(derivedKey, 0, derivedKey.size, CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedMessage(
            ciphertext = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
        )
    }

    // Descriptografa uma mensagem recebida. Usa ECDH pra derivar a chave compartilhada
    // com a chave pública do remetente, depois AES-256-GCM pra descriptografar.
    fun decrypt(
        ciphertext: String,
        nonce: String,
        senderPublicKeyBase64: String,
        recipientPrivateKeyBase64: String,
    ): String {
        val ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
        val nonceBytes = Base64.decode(nonce, Base64.NO_WRAP)

        // Decodifica a chave pública do remetente.
        val senderPublicKeyBytes = Base64.decode(senderPublicKeyBase64, Base64.NO_WRAP)
        val senderPublicKey = KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(senderPublicKeyBytes))

        // Decodifica a chave privada do destinatário.
        val recipientPrivateKeyBytes = Base64.decode(recipientPrivateKeyBase64, Base64.NO_WRAP)
        val recipientPrivateKey = KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(recipientPrivateKeyBytes))

        // Usa ECDH pra derivar a chave compartilhada (mesma que o remetente gerou).
        val keyAgreement = KeyAgreement.getInstance(AGREEMENT_ALGORITHM)
        keyAgreement.init(recipientPrivateKey)
        keyAgreement.doPhase(senderPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Deriva a mesma chave AES-256.
        val derivedKey = hkdf(sharedSecret, KEY_SIZE_BITS / 8)

        // Descriptografa com AES-256-GCM.
        val cipher = Cipher.getInstance("$CIPHER_ALGORITHM/$CIPHER_MODE/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, nonceBytes)
        val secretKey = SecretKeySpec(derivedKey, 0, derivedKey.size, CIPHER_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    // HKDF (HMAC-based Key Derivation Function) simples com SHA-256.
    private fun hkdf(ikm: ByteArray, length: Int): ByteArray {
        // Extract: MAC(salt, IKM)
        val hmac = Mac.getInstance(HMAC_ALGORITHM)
        val salt = ByteArray(32) // zero-filled salt
        hmac.init(SecretKeySpec(salt, HMAC_ALGORITHM))
        val prk = hmac.doFinal(ikm)

        // Expand: primeira iteração de PRK.
        hmac.init(SecretKeySpec(prk, HMAC_ALGORITHM))
        val t = ByteArray(0)
        val t1 = ByteArray(32)
        hmac.update(t + byteArrayOf(0x01))
        val result = hmac.doFinal()
        System.arraycopy(result, 0, t1, 0, 32)

        return t1.copyOfRange(0, length)
    }
}
