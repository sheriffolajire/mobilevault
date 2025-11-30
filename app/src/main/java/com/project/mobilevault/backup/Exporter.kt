package com.project.mobilevault.backup

import android.content.Context
import android.net.Uri
import java.io.File

object Exporter {
    fun exportAll(context: Context, dest: Uri, backupPassword: CharArray) {
        // DB bytes
        val dbPath = context.getDatabasePath("mobile_vault.db")
        val dbBytes = dbPath.readBytes()
        // Attachments (already encrypted on disk)
        val attDir = File(context.filesDir, "attachments")
        val attachments: List<Pair<String, ByteArray>> = if (attDir.exists()) {
            attDir.listFiles()?.sortedBy { it.name }?.map { it.name to it.readBytes() } ?: emptyList()
        } else emptyList()
        BackupController.export(context, dest, backupPassword, dbBytes, attachments)
    }
}