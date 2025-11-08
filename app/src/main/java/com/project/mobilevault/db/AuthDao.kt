package com.project.mobilevault.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.mobilevault.data.db.PasswordRecord

@Dao
interface AuthDao {
    @Query("SELECT * FROM auth WHERE `key` = 'master'")
    suspend fun getRecord(): PasswordRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PasswordRecord)
}
