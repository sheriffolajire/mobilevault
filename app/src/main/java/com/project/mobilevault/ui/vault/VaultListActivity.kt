
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
import com.project.mobilevault.sensors.LockController
import com.project.mobilevault.ui.editor.EntryEditorActivity
import com.project.mobilevault.ui.login.LoginActivity
import com.project.mobilevault.ui.theme.MobileVaultTheme
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.repo.SessionTimeout
import kotlinx.coroutines.delay

class VaultListActivity : ComponentActivity() {
    private val vm: VaultListViewModel by viewModels()

    private lateinit var lockController: LockController
    private val sessionTimeout = SessionTimeout(timeoutMs = 2 * 60_000L) // 2 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        lockController = LockController(this, onLock = { lockAndReturnToLogin() })

        setContent {
            MobileVaultTheme {
                val state by vm.state.collectAsState()
                LaunchedEffect(Unit) {
                    vm.load(this@VaultListActivity)
                    // Idle timeout loop regardless of movement
                    while (true) {
                        delay(1_000)
                        if (sessionTimeout.shouldTimeout()) {
                            lockAndReturnToLogin()
                            break
                        }
                    }
                }
                Scaffold { _ ->
                    VaultListScreen(
                        entries = state.items,
                        onAdd = { startActivity(Intent(this, EntryEditorActivity::class.java)) },
                        onOpen = { id -> startActivity(Intent(this, EntryEditorActivity::class.java).putExtra("entryId", id)) },
                        onLogout = { lockAndReturnToLogin() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionTimeout.poke() // reset idle timer when returning
        lockController.start()
    }

    override fun onPause() {
        super.onPause()
        lockController.stop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lockController.notifyUserInteraction()
        sessionTimeout.poke()
    }

    private fun lockAndReturnToLogin() {
        ServiceLocator.session().clear()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}
