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
    private var sessionTimeout = SessionTimeout(timeoutMs = 2 * 60_000L) // default 2 minutes; will override from settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val settings = com.project.mobilevault.settings.SettingsPrefs(this)
        lockController = LockController(this, onLock = { lockAndReturnToLogin() }, stillnessMs = settings.stillnessMs)
        sessionTimeout = com.project.mobilevault.repo.SessionTimeout(timeoutMs = settings.idleTimeoutMs)

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
                        onAdd = {
                            startActivity(Intent(this@VaultListActivity, EntryEditorActivity::class.java))
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
                        },
                        onOpen = { id ->
                            startActivity(Intent(this@VaultListActivity, EntryEditorActivity::class.java).putExtra("entryId", id))
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
                        },
                        onLogout = { lockAndReturnToLogin() },
                        onOpenSettings = {
                            startActivity(Intent(this@VaultListActivity, com.project.mobilevault.settings.SettingsActivity::class.java))
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If session was cleared while in background, return to Login
        val unlocked = runCatching { com.project.mobilevault.di.ServiceLocator.session().getDekOrThrow(); true }.getOrDefault(false)
        if (!unlocked) {
            lockAndReturnToLogin()
            return
        }

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