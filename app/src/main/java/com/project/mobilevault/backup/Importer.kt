package com.project.mobilevault.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Importer {
    fun importAll(context: Context, src: Uri, backupPassword: CharArray) {
        val result = BackupController.import(context, src, backupPassword)
        // 1) Replace DB file atomically-ish
        val dbPath = context.getDatabasePath("mobile_vault.db")
        val tmp = File(dbPath.parentFile, "mobile_vault.db.tmp")
        FileOutputStream(tmp).use { it.write(result.dbBytes) }
        if (dbPath.exists()) dbPath.delete()
        tmp.renameTo(dbPath)
        // 2) Replace attachments dir
        val attDir = File(context.filesDir, "attachments")
        attDir.mkdirs()
        attDir.listFiles()?.forEach { it.delete() }
        result.attachments.forEach { (name, bytes) ->
            File(attDir, name).outputStream().use { it.write(bytes) }
        }
    }
}