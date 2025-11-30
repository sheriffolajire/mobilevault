package com.project.mobilevault.crypto

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Streaming AES-GCM encryption/decryption for large files using chunking.
 * Each chunk gets a fresh IV and its own GCM tag. Integrity is accumulated
 * over a simple header plus all (iv || ciphertext) chunks and returned as SHA-256.
 */
object CryptoStreams {
    private const val HEADER_VERSION: Byte = 1
    private const val HEADER_RESERVED_1: Byte = 0
    private const val HEADER_RESERVED_2: Byte = 0
    private const val HEADER_RESERVED_3: Byte = 0

    private const val CHUNK_SIZE = 256 * 1024 // 256 KiB

    data class EncResult(val integrity: ByteArray, val totalBytes: Long)

    fun encryptStream(
        dek: ByteArray,
        aadBase: ByteArray,               // e.g., attachmentId||createdAt
        input: InputStream,
        output: OutputStream
    ): EncResult {
        val md = MessageDigest.getInstance("SHA-256")
        val header = byteArrayOf(HEADER_VERSION, HEADER_RESERVED_1, HEADER_RESERVED_2, HEADER_RESERVED_3)
        output.write(header)
        md.update(header)

        val buf = ByteArray(CHUNK_SIZE)
        var total = 0L
        var index = 0L
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            val chunk = if (read == buf.size) buf else buf.copyOf(read)
            val aad = ByteBuffer.allocate(aadBase.size + 8).put(aadBase).putLong(index).array()
            val ct = Crypto.encryptAesGcm(dek, chunk, aad)

            // record: [ivLen(1)] [iv] [ctLen(4)] [ct]
            output.write(byteArrayOf(ct.iv.size.toByte()))
            output.write(ct.iv)
            output.write(ByteBuffer.allocate(4).putInt(ct.bytes.size).array())
            output.write(ct.bytes)

            md.update(ct.iv)
            md.update(ct.bytes)
            total += read
            index++
        }
        return EncResult(md.digest(), total)
    }

    fun decryptStream(
        dek: ByteArray,
        aadBase: ByteArray,
        input: InputStream,
        output: OutputStream,
        expectedIntegrity: ByteArray?
    ) {
        val header = ByteArray(4)
        if (input.read(header) != 4 || header[0] != HEADER_VERSION) error("Bad header")
        val md = MessageDigest.getInstance("SHA-256").apply { update(header) }

        var index = 0L
        val oneByte = ByteArray(1)
        val lenBuf = ByteArray(4)
        while (true) {
            val ivLenRead = input.read(oneByte)
            if (ivLenRead == -1) break // EOF
            val ivLen = oneByte[0].toInt() and 0xFF
            val iv = ByteArray(ivLen)
            if (input.read(iv) != ivLen) error("Truncated IV")
            if (input.read(lenBuf) != 4) error("Truncated length")
            val ctLen = ByteBuffer.wrap(lenBuf).int
            val ct = ByteArray(ctLen)
            if (input.read(ct) != ctLen) error("Truncated ciphertext")

            md.update(iv); md.update(ct)
            val aad = ByteBuffer.allocate(aadBase.size + 8).put(aadBase).putLong(index).array()
            val pt = Crypto.decryptAesGcm(dek, iv, ct, aad)
            output.write(pt)
            index++
        }
        val integ = md.digest()
        if (expectedIntegrity != null && !integ.contentEquals(expectedIntegrity)) error("Integrity mismatch")
    }
}