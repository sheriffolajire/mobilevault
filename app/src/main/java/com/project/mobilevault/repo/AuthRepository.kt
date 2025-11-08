package com.project.mobilevault.repo

import com.project.mobilevault.crypto.Crypto
import com.project.mobilevault.data.db.AuthDao
import com.project.mobilevault.data.db.PasswordRecord

class AuthRepository(private val authDao: AuthDao) {
    data class SetupResult(val wrapped: PasswordRecord)

    suspend fun isInitialized(): Boolean = authDao.getRecord() != null

    suspend fun setupNewPassword(password: CharArray, iterations: Int = 210_000): SetupResult {
        val salt = Crypto.randomBytes(16)
        val kek = Crypto.pbkdf2(password, salt, iterations)
        val dek = Crypto.randomBytes(32)
        val verifier = Crypto.hmaclessVerifier(kek)
        val wrapped = Crypto.encryptAesGcm(kek, dek)
        dek.fill(0)
        val record = PasswordRecord(
            key = "master",
            salt = salt,
            iterations = iterations,
            verifier = verifier,
            wrappedDekIv = wrapped.iv,
            wrappedDekCiphertext = wrapped.bytes,
        )
        authDao.upsert(record)
        return SetupResult(record)
    }

    suspend fun tryLogin(password: CharArray): ByteArray? {
        val rec = authDao.getRecord() ?: return null
        val kek = Crypto.pbkdf2(password, rec.salt, rec.iterations)
        val check = Crypto.hmaclessVerifier(kek)
        if (!check.contentEquals(rec.verifier)) return null
        return Crypto.decryptAesGcm(kek, rec.wrappedDekIv, rec.wrappedDekCiphertext)
    }
}
