package com.project.mobilevault.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BioKeystore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "mv_bio_aes"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
        // Optionally you can enable a validity duration to allow grace period unlocks
        //.setUserAuthenticationValidityDurationSeconds(30)
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        kg.init(builder.build())
        return kg.generateKey()
    }

    fun initEncryptCipher(): Pair<Cipher, ByteArray> {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher to cipher.iv
    }

    fun initDecryptCipher(iv: ByteArray): Cipher {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORM)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
}