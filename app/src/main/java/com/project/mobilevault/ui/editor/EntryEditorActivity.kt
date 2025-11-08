package com.project.mobilevault.ui.editor

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.project.mobilevault.ui.theme.MobileVaultTheme

class EntryEditorActivity : ComponentActivity() {
    private val vm: EntryEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val entryId = intent.getLongExtra("entryId", -1L).takeIf { it > 0 }
        setContent {
            MobileVaultTheme {
                LaunchedEffect(Unit) { vm.load(this@EntryEditorActivity, entryId) }
                EntryEditorScreen(
                    state = vm.state,
                    onSave = { title, content -> vm.save(this@EntryEditorActivity, entryId, title, content) { finish() } },
                    onCancel = { finish() }
                )
            }
        }
    }
}
