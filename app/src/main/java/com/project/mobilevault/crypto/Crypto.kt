package com.project.mobilevault.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private const val AES = "AES"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2 = "PBKDF2WithHmacSHA256"

    data class CipherText(val iv: ByteArray, val bytes: ByteArray)

    fun randomBytes(len: Int): ByteArray = ByteArray(len).also { SecureRandom().nextBytes(it) }

    fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLen: Int = 32): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLen * 8)
        return SecretKeyFactory.getInstance(PBKDF2).generateSecret(spec).encoded
    }

    fun hmaclessVerifier(derivedKey: ByteArray): ByteArray = sha256(derivedKey)

    fun sha256(vararg parts: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        parts.forEach { md.update(it) }
        return md.digest()
    }

    fun encryptAesGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): CipherText {
        val iv = randomBytes(12)
        val sk = SecretKeySpec(key, AES)
        val cipher = Cipher.getInstance(TRANSFORM)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, sk, spec)
        if (aad != null) cipher.updateAAD(aad)
        val out = cipher.doFinal(plaintext)
        return CipherText(iv, out)
    }

    fun decryptAesGcm(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        val sk = SecretKeySpec(key, AES)
        val cipher = Cipher.getInstance(TRANSFORM)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, sk, spec)
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }
}