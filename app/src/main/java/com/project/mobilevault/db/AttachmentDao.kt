package com.project.mobilevault.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE entryId = :entryId ORDER BY updatedAt DESC")
    fun observeForEntry(entryId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): Attachment?

    @Insert
    suspend fun insert(a: Attachment): Long

    @Update
    suspend fun update(a: Attachment)

    @Delete
    suspend fun delete(a: Attachment)
}