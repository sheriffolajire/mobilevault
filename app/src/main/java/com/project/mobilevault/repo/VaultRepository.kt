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
            val aad = aadFor(e.id, e.createdAt) // bind to id + createdAt
            // Verify integrity before decrypt
            val computedIntegrity = Crypto.sha256(aad, e.iv, e.ciphertext)
            if (!computedIntegrity.contentEquals(e.integrity)) return null
            val dek = session.getDekOrThrow()
            val plain = Crypto.decryptAesGcm(dek, e.iv, e.ciphertext, aad)
            e to plain.decodeToString()
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun upsertEncrypted(id: Long?, title: String, content: String): Long {
        val now = System.currentTimeMillis()
        val dek = session.getDekOrThrow()
        return if (id == null) {
            // Two-step insert to bind AAD to generated id + createdAt
            val created = now
            // Step 1: insert placeholder to get id
            val placeholder = VaultEntry(
                id = 0L,
                title = title,
                createdAt = created,
                updatedAt = now,
                iv = ByteArray(0),
                ciphertext = ByteArray(0),
                integrity = ByteArray(0)
            )
            val newId = db.vaultDao().insert(placeholder)
            // Step 2: encrypt using AAD(id + createdAt)
            val aad = aadFor(newId, created)
            val ct = Crypto.encryptAesGcm(dek, content.encodeToByteArray(), aad)
            val integrity = Crypto.sha256(aad, ct.iv, ct.bytes)
            val final = VaultEntry(
                id = newId,
                title = title,
                createdAt = created,
                updatedAt = now,
                iv = ct.iv,
                ciphertext = ct.bytes,
                integrity = integrity
            )
            db.vaultDao().update(final)
            newId
        } else {
            val existing = db.vaultDao().getById(id)
            val created = existing?.createdAt ?: now
            val aad = aadFor(id, created)
            val ct = Crypto.encryptAesGcm(dek, content.encodeToByteArray(), aad)
            val integrity = Crypto.sha256(aad, ct.iv, ct.bytes)
            val entry = VaultEntry(
                id = id,
                title = title,
                createdAt = created,
                updatedAt = now,
                iv = ct.iv,
                ciphertext = ct.bytes,
                integrity = integrity
            )
            db.vaultDao().update(entry)
            id
        }
    }

    suspend fun delete(entry: VaultEntry) = db.vaultDao().delete(entry)

    private fun aadFor(id: Long, createdAt: Long): ByteArray =
        ByteBuffer.allocate(16).putLong(id).putLong(createdAt).array()
}