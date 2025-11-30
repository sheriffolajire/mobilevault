package com.project.mobilevault.security

import com.project.mobilevault.crypto.Crypto
import com.project.mobilevault.data.db.AuthDao
import com.project.mobilevault.data.db.PasswordRecord

/**
 * Rotates the Key Encryption Key (KEK) by changing the master password
 * without re-encrypting the database contents. The DEK remains the same
 * and is only re-wrapped under the new KEK.
 */
class PasswordChangeManager(private val authDao: AuthDao) {
    suspend fun changePassword(current: CharArray, next: CharArray, iterations: Int = 210_000): Boolean {
        val rec = authDao.getRecord() ?: return false
        // Derive old KEK and verify
        val oldKek = Crypto.pbkdf2(current, rec.salt, rec.iterations)
        val check = Crypto.hmaclessVerifier(oldKek)
        if (!check.contentEquals(rec.verifier)) return false
        // Unwrap DEK
        val dek = Crypto.decryptAesGcm(oldKek, rec.wrappedDekIv, rec.wrappedDekCiphertext)
        // Derive new KEK with new salt
        val newSalt = Crypto.randomBytes(16)
        val newKek = Crypto.pbkdf2(next, newSalt, iterations)
        val newVerifier = Crypto.hmaclessVerifier(newKek)
        val wrapped = Crypto.encryptAesGcm(newKek, dek)
        dek.fill(0)
        val updated = PasswordRecord(
            key = rec.key,
            salt = newSalt,
            iterations = iterations,
            verifier = newVerifier,
            wrappedDekIv = wrapped.iv,
            wrappedDekCiphertext = wrapped.bytes
        )
        authDao.upsert(updated)
        return true
    }
}