package com.project.mobilevault.repo

class SessionKeyHolder {
    @Volatile private var dek: ByteArray? = null

    fun setDek(bytes: ByteArray) { dek = bytes }
    fun getDekOrThrow(): ByteArray = dek ?: error("Locked")
    fun isUnlocked(): Boolean = dek != null
    fun clear() { dek?.fill(0); dek = null }
}
