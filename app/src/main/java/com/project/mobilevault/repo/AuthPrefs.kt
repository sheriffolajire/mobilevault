package com.project.mobilevault.repo

import android.content.Context
import android.util.Base64

class AuthPrefs(context: Context) {
    private val sp = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveBiometricWrappedDek(iv: ByteArray, blob: ByteArray) {
        sp.edit()
            .putString(KEY_BIO_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_BIO_BLOB, Base64.encodeToString(blob, Base64.NO_WRAP))
            .apply()
    }

    fun loadBiometricWrappedDek(): Pair<ByteArray, ByteArray>? {
        val ivB64 = sp.getString(KEY_BIO_IV, null) ?: return null
        val blobB64 = sp.getString(KEY_BIO_BLOB, null) ?: return null
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val blob = Base64.decode(blobB64, Base64.NO_WRAP)
        return iv to blob
    }

    fun hasBiometricWrappedDek(): Boolean = sp.contains(KEY_BIO_IV) && sp.contains(KEY_BIO_BLOB)

    fun clearBiometricWrappedDek() {
        sp.edit().remove(KEY_BIO_IV).remove(KEY_BIO_BLOB).apply()
    }

    companion object {
        private const val KEY_BIO_IV = "bio_iv"
        private const val KEY_BIO_BLOB = "bio_blob"
    }
}