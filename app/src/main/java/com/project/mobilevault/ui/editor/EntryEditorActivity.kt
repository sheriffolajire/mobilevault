package com.project.mobilevault.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.project.mobilevault.data.db.Attachment
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.ui.theme.MobileVaultTheme
import kotlinx.coroutines.launch

class EntryEditorActivity : ComponentActivity() {
    private val vm: EntryEditorViewModel by viewModels()

    private var currentEntryId: Long? = null

    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val id = currentEntryId ?: return@registerForActivityResult
        lifecycleScope.launch {
            vm.importAttachments(this@EntryEditorActivity, id, uris)
        }
    }

    private fun ensureEntryThenPick(title: String, content: String) {
        lifecycleScope.launch {
            try {
                if (currentEntryId == null) {
                    val repo = ServiceLocator.vaultRepo(this@EntryEditorActivity)
                    val newId = repo.upsertEncrypted(
                        id = null,
                        title = if (title.isBlank()) "Untitled" else title,
                        content = content
                    )
                    currentEntryId = newId
                    vm.load(this@EntryEditorActivity, newId)
                }
                pickFiles.launch(arrayOf("*/*"))
            } catch (_: Throwable) {
                // ignore; UI will surface errors through ViewModel if needed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        currentEntryId = intent.getLongExtra("entryId", -1L).takeIf { it > 0 }
        val entryId = currentEntryId
        setContent {
            MobileVaultTheme {
                LaunchedEffect(Unit) { vm.load(this@EntryEditorActivity, entryId) }
                EntryEditorScreen(
                    state = vm.state,
                    onSave = { title, content ->
                        val idForSave = currentEntryId ?: entryId
                        vm.save(this@EntryEditorActivity, idForSave, title, content) { finish() }
                    },
                    onCancel = { finish() },
                    onAddAttachment = { title, content -> ensureEntryThenPick(title, content) },
                    onOpenAttachment = { att -> openAttachment(att) },
                    onDeleteAttachment = { att -> deleteAttachment(att) }
                )
            }
        }
    }

    private fun openAttachment(att: Attachment) {
        // Prefer decrypting ContentProvider to avoid plaintext cache
        val uri = Uri.parse("content://${packageName}.viewer/attachment/${att.id}")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, att.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure {
            // Fallback: try cache-based decrypt if no app accepts our content provider
            lifecycleScope.launch {
                try {
                    val repo = ServiceLocator.attachmentRepo(this@EntryEditorActivity)
                    val cacheUri = repo.decryptToCache(att.id) ?: return@launch
                    val fallback = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(cacheUri, att.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(fallback)
                } catch (_: Throwable) { }
            }
        }
    }

    private fun deleteAttachment(att: Attachment) {
        lifecycleScope.launch {
            try {
                val repo = ServiceLocator.attachmentRepo(this@EntryEditorActivity)
                repo.deleteAttachment(att)
            } catch (_: Throwable) { }
        }
    }
}