package com.project.mobilevault.repo

import com.project.mobilevault.crypto.Crypto
import com.project.mobilevault.data.db.AppDb
import com.project.mobilevault.data.db.VaultEntry
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

class VaultRepository(
    private val db: AppDb,
    private val session: SessionKeyHolder
) {
    fun entries(): Flow<List<VaultEntry>> = db.vaultDao().observeEntries()

    suspend fun getDecrypted(id: Long): Pair<VaultEntry, String>? {
        val e = db.vaultDao().getById(id) ?: return null
        return try {
            val aad = aadFor(e.createdAt) // bind only to createdAt to avoid id-mismatch on first insert
            val dek = session.getDekOrThrow()
            val plain = Crypto.decryptAesGcm(dek, e.iv, e.ciphertext, aad)
            e to plain.decodeToString()
        } catch (t: Throwable) {
            // Return null on decryption failure (legacy/corrupt rows)
            null
        }
    }

    suspend fun upsertEncrypted(id: Long?, title: String, content: String): Long {
        val now = System.currentTimeMillis()
        val created = if (id == null) now else (db.vaultDao().getById(id)?.createdAt ?: now)
        val aad = aadFor(created) // only createdAt
        val dek = session.getDekOrThrow()
        val ct = Crypto.encryptAesGcm(dek, content.encodeToByteArray(), aad)
        val entry = VaultEntry(
            id = id ?: 0L,
            title = title,
            createdAt = created,
            updatedAt = now,
            iv = ct.iv,
            ciphertext = ct.bytes,
        )
        return if (id == null) db.vaultDao().insert(entry) else { db.vaultDao().update(entry); id }
    }

    suspend fun delete(entry: VaultEntry) = db.vaultDao().delete(entry)

    private fun aadFor(createdAt: Long): ByteArray =
        ByteBuffer.allocate(8).putLong(createdAt).array()
}
