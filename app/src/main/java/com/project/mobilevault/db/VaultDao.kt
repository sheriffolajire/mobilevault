package com.project.mobilevault.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT id, title, createdAt, updatedAt, iv, ciphertext, integrity FROM vault_entries ORDER BY updatedAt DESC")
    fun observeEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntry?

    @Insert
    suspend fun insert(entry: VaultEntry): Long

    @Update
    suspend fun update(entry: VaultEntry)

    @Delete
    suspend fun delete(entry: VaultEntry)
}
