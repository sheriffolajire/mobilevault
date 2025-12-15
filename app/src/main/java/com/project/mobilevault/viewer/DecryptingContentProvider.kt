package com.project.mobilevault.viewer

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.project.mobilevault.di.ServiceLocator
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer

/**
 * Streams decrypted attachment bytes directly via a pipe without writing plaintext to disk.
 * URI format: content://{authority}/attachment/{id}
 */
class DecryptingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? {
        val segments = uri.pathSegments
        if (segments.size == 2 && segments[0] == "attachment") {
            val id = segments[1].toLongOrNull() ?: return null
            val ctx = context ?: return null
            val repo = ServiceLocator.attachmentRepo(ctx)
            val att = runBlocking { repo.getById(id) }
            return att?.mimeType
        }
        return null
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "attachment") return null
        val id = segments[1].toLongOrNull() ?: return null
        val ctx = context ?: return null
        val repo = ServiceLocator.attachmentRepo(ctx)
        val session = ServiceLocator.session()

        val pipe = ParcelFileDescriptor.createPipe()
        val readFd = pipe[0]
        val writeFd = pipe[1]
        val out = ParcelFileDescriptor.AutoCloseOutputStream(writeFd)

        Thread {
            runCatching {
                val a = runBlocking { repo.getById(id) } ?: return@runCatching
                val dek = session.getDekOrThrow()
                val aadBase = ByteBuffer.allocate(16).putLong(a.id).putLong(a.createdAt).array()
                val encFile = File(a.path)
                FileInputStream(encFile).use { inS ->
                    // Use CryptoStreams from repo; integrity verified
                    com.project.mobilevault.crypto.CryptoStreams.decryptStream(dek, aadBase, inS, out, a.integrity)
                }
            }.getOrElse {
                // Swallow errors to avoid leaking details
            }
            runCatching { out.close() }
        }.start()
        return readFd
    }
}