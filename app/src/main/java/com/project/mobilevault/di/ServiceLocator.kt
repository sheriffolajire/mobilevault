package com.project.mobilevault.di

import android.content.Context
import com.project.mobilevault.data.db.AppDb
import com.project.mobilevault.repo.*

object ServiceLocator {
    @Volatile private var session: SessionKeyHolder? = null

    fun session(): SessionKeyHolder = session ?: synchronized(this) {
        session ?: SessionKeyHolder().also { session = it }
    }

    fun authRepo(context: Context) = AuthRepository(AppDb.get(context).authDao())
    fun vaultRepo(context: Context) = VaultRepository(AppDb.get(context), session())
    fun attachmentRepo(context: Context) = AttachmentRepository(AppDb.get(context), session(), context)
}