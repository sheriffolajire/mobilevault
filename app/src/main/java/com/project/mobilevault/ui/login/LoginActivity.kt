package com.project.mobilevault.ui.login

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.project.mobilevault.ui.theme.MobileVaultTheme
import com.project.mobilevault.ui.vault.VaultListActivity

class LoginActivity : ComponentActivity() {
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            MobileVaultTheme {
                // Probe DB so UI decides between Create vs Unlock immediately
                LaunchedEffect(Unit) { vm.refreshInitialized(this@LoginActivity) }

                val state by vm.state.collectAsState()
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        LoginScreen(
                            state = state,
                            onSubmit = { pass, confirm -> vm.onSubmit(this@LoginActivity, pass, confirm, ::goToVault) }
                        )
                    }
                }
            }
        }
    }

    private fun goToVault() {
        startActivity(Intent(this, VaultListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}
