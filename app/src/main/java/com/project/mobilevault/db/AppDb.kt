package com.project.mobilevault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VaultEntry::class, PasswordRecord::class],
    version = 1,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun authDao(): AuthDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "mobile_vault.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
