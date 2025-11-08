package com.project.mobilevault.ui.vault

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.project.mobilevault.ui.editor.EntryEditorActivity
import com.project.mobilevault.ui.theme.MobileVaultTheme

class VaultListActivity : ComponentActivity() {
    private val vm: VaultListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            MobileVaultTheme {
                val state by vm.state.collectAsState()
                LaunchedEffect(Unit) { vm.load(this@VaultListActivity) }
                Scaffold { _ ->
                    VaultListScreen(
                        entries = state.items,
                        onAdd = { startActivity(Intent(this, EntryEditorActivity::class.java)) },
                        onOpen = { id -> startActivity(Intent(this, EntryEditorActivity::class.java).putExtra("entryId", id)) },
                        onLogout = { vm.logout(this) }
                    )
                }
            }
        }
    }
}
