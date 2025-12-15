package com.project.mobilevault.util

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore

object SourceDeletion {
    fun isMediaStoreUri(uri: Uri): Boolean =
        uri.authority == "media" || uri.toString().startsWith("content://media/")

    fun isDocumentUri(activity: Activity, uri: Uri): Boolean =
        DocumentsContract.isDocumentUri(activity, uri)

    /** Best-effort delete for a single DocumentsProvider URI (no user prompt). */
    fun deleteDocumentIfPossible(activity: Activity, uri: Uri): Boolean {
        return runCatching {
            DocumentsContract.deleteDocument(activity.contentResolver, uri)
        }.getOrElse { false }
    }

    /**
     * Build an IntentSender to request deletion of the given MediaStore URIs with system UI (Android 11+).
     * Returns null if not supported or building failed.
     */
    fun buildMediaStoreDeleteIntentSender(cr: ContentResolver, uris: List<Uri>) = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(cr, uris).intentSender
        } else null
    }.getOrNull()
}