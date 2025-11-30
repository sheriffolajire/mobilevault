package com.project.mobilevault.backup

import android.content.Context
import android.net.Uri
import com.project.mobilevault.crypto.Crypto
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Streaming backup controller. The outer header is plaintext and contains KDF
 * params to derive the backup key. The inner archive is AES-GCM encrypted and
 * contains simple length-prefixed records.
 */
object BackupController {
    data class Header(
        val version: Int = 1,
        val createdAt: Long,
        val salt: ByteArray,
        val iterations: Int
    ) {
        fun toBytes(): ByteArray {
            val bb = ByteBuffer.allocate(4 + 8 + 4 + salt.size)
            bb.putInt(version)
            bb.putLong(createdAt)
            bb.putInt(iterations)
            bb.put(salt)
            return bb.array()
        }

        companion object {
            fun fromBytes(bytes: ByteArray): Header {
                val bb = ByteBuffer.wrap(bytes)
                val ver = bb.int
                val ts = bb.long
                val it = bb.int
                val salt = ByteArray(bb.remaining())
                bb.get(salt)
                return Header(ver, ts, salt, it)
            }
        }
    }

    private fun writeRecord(out: OutputStream, name: String, data: ByteArray) {
        val n = name.toByteArray()
        out.write(ByteBuffer.allocate(4).putInt(n.size).array())
        out.write(n)
        out.write(ByteBuffer.allocate(8).putLong(data.size.toLong()).array())
        out.write(data)
    }

    private fun readRecord(`in`: InputStream): Pair<String, ByteArray>? {
        val nameLenBuf = ByteArray(4)
        val r = `in`.read(nameLenBuf)
        if (r == -1) return null
        if (r != 4) error("Truncated archive")
        val nameLen = ByteBuffer.wrap(nameLenBuf).int
        val nameBytes = ByteArray(nameLen)
        if (`in`.read(nameBytes) != nameLen) error("Truncated name")
        val dataLenBuf = ByteArray(8)
        if (`in`.read(dataLenBuf) != 8) error("Truncated length")
        val dataLen = ByteBuffer.wrap(dataLenBuf).long
        if (dataLen < 0L || dataLen > Int.MAX_VALUE) error("Data too large in record")
        val data = ByteArray(dataLen.toInt())
        var off = 0
        while (off < data.size) {
            val rr = `in`.read(data, off, data.size - off)
            if (rr <= 0) error("Truncated data")
            off += rr
        }
        return String(nameBytes) to data
    }

    fun export(
        context: Context,
        dest: Uri,
        backupPassword: CharArray,
        dbBytes: ByteArray,
        attachments: List<Pair<String, ByteArray>>
    ) {
        val salt = Crypto.randomBytes(16)
        val iterations = 210_000
        val key = Crypto.pbkdf2(backupPassword, salt, iterations)
        val header = Header(createdAt = System.currentTimeMillis(), salt = salt, iterations = iterations)

        context.contentResolver.openOutputStream(dest)!!.use { baseOut ->
            val hdr = header.toBytes()
            baseOut.write(ByteBuffer.allocate(4).putInt(hdr.size).array())
            baseOut.write(hdr)

            // Build inner archive in memory for simplicity (DB + attachments)
            val archive = java.io.ByteArrayOutputStream()
            writeRecord(archive, "db.sqlite", dbBytes)
            attachments.forEach { (name, bytes) ->
                writeRecord(archive, "att/$name", bytes)
            }
            val inner = archive.toByteArray()
            val aad = ByteBuffer.allocate(8).putLong(header.createdAt).array()
            val ct = Crypto.encryptAesGcm(key, inner, aad)
            baseOut.write(ByteBuffer.allocate(4).putInt(ct.iv.size).array())
            baseOut.write(ct.iv)
            baseOut.write(ByteBuffer.allocate(4).putInt(ct.bytes.size).array())
            baseOut.write(ct.bytes)
        }
    }

    data class ImportResult(val dbBytes: ByteArray, val attachments: List<Pair<String, ByteArray>>)

    fun import(context: Context, src: Uri, backupPassword: CharArray): ImportResult {
        context.contentResolver.openInputStream(src)!!.use { baseIn ->
            val lenBuf = ByteArray(4)
            if (baseIn.read(lenBuf) != 4) error("Bad header len")
            val hdrLen = ByteBuffer.wrap(lenBuf).int
            val hdrBytes = ByteArray(hdrLen)
            if (baseIn.read(hdrBytes) != hdrLen) error("Bad header")
            val header = Header.fromBytes(hdrBytes)
            val key = Crypto.pbkdf2(backupPassword, header.salt, header.iterations)
            val aad = ByteBuffer.allocate(8).putLong(header.createdAt).array()

            val ivLenBuf = ByteArray(4)
            if (baseIn.read(ivLenBuf) != 4) error("Bad IV len")
            val ivLen = ByteBuffer.wrap(ivLenBuf).int
            val iv = ByteArray(ivLen)
            if (baseIn.read(iv) != ivLen) error("Bad IV")
            val ctLenBuf = ByteArray(4)
            if (baseIn.read(ctLenBuf) != 4) error("Bad CT len")
            val ctLen = ByteBuffer.wrap(ctLenBuf).int
            val ct = ByteArray(ctLen)
            var off = 0
            while (off < ctLen) {
                val r2 = baseIn.read(ct, off, ctLen - off)
                if (r2 <= 0) error("Truncated CT")
                off += r2
            }
            val inner = Crypto.decryptAesGcm(key, iv, ct, aad)
            val inBuf = inner.inputStream()
            var db: ByteArray? = null
            val atts = mutableListOf<Pair<String, ByteArray>>()
            while (true) {
                val rec = readRecord(inBuf) ?: break
                val (name, data) = rec
                if (name == "db.sqlite") db = data else if (name.startsWith("att/")) atts += name.removePrefix("att/") to data
            }
            return ImportResult(db ?: error("No DB in backup"), atts)
        }
    }
}