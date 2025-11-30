package com.project.mobilevault.repo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.project.mobilevault.crypto.CryptoStreams
import com.project.mobilevault.data.db.AppDb
import com.project.mobilevault.data.db.Attachment
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AttachmentRepository(
    private val db: AppDb,
    private val session: SessionKeyHolder,
    private val context: Context
) {
    fun observeForEntry(entryId: Long): Flow<List<Attachment>> =
        db.attachmentDao().observeForEntry(entryId)

    suspend fun getById(id: Long): Attachment? = db.attachmentDao().getById(id)

    suspend fun importForEntry(entryId: Long?, src: Uri): Long {
        val cr = context.contentResolver
        val name = queryDisplayName(cr, src) ?: "document"
        val mime = cr.getType(src) ?: "application/octet-stream"
        val now = System.currentTimeMillis()

        val placeholder = Attachment(
            id = 0L,
            entryId = entryId,
            displayName = name,
            mimeType = mime,
            createdAt = now,
            updatedAt = now,
            sizeBytes = 0L,
            path = "",
            integrity = ByteArray(0)
        )
        val id = db.attachmentDao().insert(placeholder)

        val aadBase = ByteBuffer.allocate(16).putLong(id).putLong(now).array()
        val dek = session.getDekOrThrow()
        val destFile = File(context.filesDir, "attachments/${id}.enc").apply { parentFile?.mkdirs() }
        cr.openInputStream(src)!!.use { inS ->
            FileOutputStream(destFile).use { outS ->
                val res = CryptoStreams.encryptStream(dek, aadBase, inS, outS)
                val final = placeholder.copy(
                    id = id,
                    path = destFile.absolutePath,
                    sizeBytes = res.totalBytes,
                    integrity = res.integrity,
                    updatedAt = System.currentTimeMillis()
                )
                db.attachmentDao().update(final)
            }
        }
        return id
    }

    suspend fun decryptToCache(attId: Long): Uri? {
        val a = db.attachmentDao().getById(attId) ?: return null
        val dek = session.getDekOrThrow()
        val aadBase = ByteBuffer.allocate(16).putLong(a.id).putLong(a.createdAt).array()
        val src = File(a.path)
        if (!src.exists()) return null
        val cache = File(context.cacheDir, "decrypted/$attId")
        cache.parentFile?.mkdirs()
        FileInputStream(src).use { inS ->
            FileOutputStream(cache).use { outS ->
                CryptoStreams.decryptStream(dek, aadBase, inS, outS, a.integrity)
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", cache)
    }

    suspend fun deleteAttachment(att: Attachment) {
        runCatching { File(att.path).delete() }
        db.attachmentDao().delete(att)
    }

    private fun queryDisplayName(cr: ContentResolver, uri: Uri): String? {
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) return c.getString(idx)
        }
        return null
    }
}