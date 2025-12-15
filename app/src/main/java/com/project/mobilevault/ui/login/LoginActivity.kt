package com.project.mobilevault.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.project.mobilevault.ui.theme.MobileVaultTheme
import com.project.mobilevault.ui.vault.VaultListActivity
import com.project.mobilevault.biometric.BioKeystore
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.repo.AuthPrefs

class LoginActivity : AppCompatActivity() {
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
                LaunchedEffect(Unit) {
                    vm.refreshInitialized(this@LoginActivity)
                    // Try biometric quick unlock if possible
                    tryBiometricQuickUnlock()
                }

                val state by vm.state.collectAsState()
                val prefsSettings = com.project.mobilevault.settings.SettingsPrefs(this@LoginActivity)
                val prefsAuth = AuthPrefs(this@LoginActivity)
                val canBio = BiometricManager.from(this@LoginActivity)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                val showBiometric = prefsSettings.biometricsEnabled && canBio && prefsAuth.hasBiometricWrappedDek()

                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        LoginScreen(
                            state = state,
                            onSubmit = { pass, confirm ->
                                vm.onSubmit(this@LoginActivity, pass, confirm) {
                                    // After password unlock, offer/enable biometric quick unlock by wrapping DEK if enabled in settings
                                    if (prefsSettings.biometricsEnabled) enableBiometricQuickUnlockIfAvailable()
                                    goToVault()
                                }
                            },
                            showBiometric = showBiometric,
                            onBiometricClick = { tryBiometricQuickUnlock() }
                        )
                    }
                }
            }
        }
    }

    private fun tryBiometricQuickUnlock() {
        val settings = com.project.mobilevault.settings.SettingsPrefs(this)
        if (!settings.biometricsEnabled) return
        val prefs = AuthPrefs(this)
        val pair = prefs.loadBiometricWrappedDek() ?: return
        val (iv, blob) = pair
        val can = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) return
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Use biometrics to re-unlock")
            .setNegativeButtonText("Use password")
            .build()
        val cipher = try { BioKeystore.initDecryptCipher(iv) } catch (_: Throwable) { return }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val dek = result.cryptoObject!!.cipher!!.doFinal(blob)
                    ServiceLocator.session().setDek(dek)
                    dek.fill(0)
                    goToVault()
                } catch (_: Throwable) {
                    // fall back silently
                }
            }
        })
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun enableBiometricQuickUnlockIfAvailable() {
        val can = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) return
        val prefs = AuthPrefs(this)
        if (prefs.hasBiometricWrappedDek()) return
        // Wrap current session DEK with keystore and store
        try {
            val dek = ServiceLocator.session().getDekOrThrow()
            val (cipher, iv) = BioKeystore.initEncryptCipher()
            val blob = cipher.doFinal(dek)
            prefs.saveBiometricWrappedDek(iv, blob)
        } catch (_: Throwable) {
            // ignore
        }
    }

    private fun goToVault() {
        startActivity(Intent(this, VaultListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        // An entrance transition for better motion
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
    }
}