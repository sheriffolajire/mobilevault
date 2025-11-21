package com.project.mobilevault.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    // AES-GCM fields
    val iv: ByteArray,
    val ciphertext: ByteArray,
    // Integrity metadata: SHA-256 over (aad || iv || ciphertext)
    val integrity: ByteArray,
)

@Entity(tableName = "auth")
data class PasswordRecord(
    @PrimaryKey val key: String = "master",
    val salt: ByteArray,
    val iterations: Int,
    val verifier: ByteArray,
    val wrappedDekIv: ByteArray,
    val wrappedDekCiphertext: ByteArray,
)
