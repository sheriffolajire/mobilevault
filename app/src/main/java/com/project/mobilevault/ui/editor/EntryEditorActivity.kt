package com.project.mobilevault.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.project.mobilevault.data.db.Attachment
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.settings.SettingsPrefs
import com.project.mobilevault.ui.theme.MobileVaultTheme
import com.project.mobilevault.util.SourceDeletion
import kotlinx.coroutines.launch

class EntryEditorActivity : ComponentActivity() {
    private val vm: EntryEditorViewModel by viewModels()

    private var currentEntryId: Long? = null

    private val confirmDeleteMedia = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { _ ->
        // No-op; system shows result, and MediaStore handles deletion
    }

    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val id = currentEntryId ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                // Persist permissions best-effort
                uris.forEach { u ->
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            u,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                }
                // Import sequentially so we know when it completes
                val attRepo = ServiceLocator.attachmentRepo(this@EntryEditorActivity)
                uris.forEach { u -> attRepo.importForEntry(id, u) }

                // Auto-delete originals if enabled in settings; otherwise, prompt the user
                val settings = SettingsPrefs(this@EntryEditorActivity)
                if (settings.autoDeleteOnImport) {
                    performDeletionForUris(uris)
                } else {
                    maybePromptDeleteOriginals(uris)
                }
            } catch (_: Throwable) { }
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

    private fun maybePromptDeleteOriginals(uris: List<Uri>) {
        if (uris.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Delete originals?")
            .setMessage("Your files have been imported into the vault. Do you want to delete the originals from this device?")
            .setNegativeButton("Keep") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ ->
                d.dismiss()
                performDeletionForUris(uris)
            }
            .show()
    }

    private fun performDeletionForUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val docs = mutableListOf<Uri>()
        val media = mutableListOf<Uri>()
        uris.forEach { u ->
            when {
                SourceDeletion.isMediaStoreUri(u) -> media += u
                SourceDeletion.isDocumentUri(this, u) -> docs += u
            }
        }
        // Best-effort: delete docs directly
        docs.forEach { u -> SourceDeletion.deleteDocumentIfPossible(this, u) }
        // MediaStore: request user-confirmed deletion on Android 11+. Pre-R: try direct delete.
        val sender = SourceDeletion.buildMediaStoreDeleteIntentSender(contentResolver, media)
        if (sender != null) {
            val req = IntentSenderRequest.Builder(sender).build()
            confirmDeleteMedia.launch(req)
        } else {
            media.forEach { u -> runCatching { contentResolver.delete(u, null, null) } }
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
                        vm.save(this@EntryEditorActivity, idForSave, title, content) { finishWithAnim() }
                    },
                    onCancel = { finishWithAnim() },
                    onAddAttachment = { title, content -> ensureEntryThenPick(title, content) },
                    onOpenAttachment = { att -> openAttachment(att) },
                    onDeleteAttachment = { att -> deleteAttachment(att) }
                )
            }
        }
    }

    private fun openAttachment(att: Attachment) {
        // Inline viewer Activity using ContentProvider stream to avoid plaintext cache
        val intent = Intent(this, com.project.mobilevault.viewer.AttachmentViewerActivity::class.java).apply {
            putExtra("attId", att.id)
            putExtra("mime", att.mimeType)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
    }

    private fun deleteAttachment(att: Attachment) {
        lifecycleScope.launch {
            try {
                val repo = ServiceLocator.attachmentRepo(this@EntryEditorActivity)
                repo.deleteAttachment(att)
            } catch (_: Throwable) { }
        }
    }

    override fun onResume() {
        super.onResume()
        val unlocked = runCatching { com.project.mobilevault.di.ServiceLocator.session().getDekOrThrow(); true }.getOrDefault(false)
        if (!unlocked) {
            finish()
            startActivity(Intent(this, com.project.mobilevault.ui.login.LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

private fun EntryEditorActivity.finishWithAnim() {
    finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right)
}