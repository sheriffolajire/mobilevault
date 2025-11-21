package com.project.mobilevault.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginViewModel.UiState,
    onSubmit: (String, String?) -> Unit,
    showBiometric: Boolean = false,
    onBiometricClick: () -> Unit = {}
) {
    val focus = LocalFocusManager.current

    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val isCreate = !state.isInitialized
    val isError = state.error != null

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(if (isCreate) "Create Master Password" else "Unlock Vault") }) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Centered card for better focus
            ElevatedCard(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.Center)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isCreate) "Set a strong password to protect your vault." else "Enter your master password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = isError,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isCreate) {
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            isError = isError || (confirm.isNotEmpty() && confirm != password),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isError) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            focus.clearFocus(force = true)
                            onSubmit(password, if (isCreate) confirm else null)
                        },
                        enabled = password.isNotBlank() && (!isCreate || password == confirm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isCreate) "Create & Unlock" else "Unlock")
                    }

                    if (showBiometric && !isCreate) {
                        OutlinedButton(onClick = onBiometricClick, modifier = Modifier.fillMaxWidth()) {
                            Text("Unlock with biometrics")
                        }
                    }

                    // Tiny helper text
                    if (isCreate) {
                        Text(
                            text = "Tip: Use 4+ words or 12+ chars. Keep it private.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Optional loading state (wire up to state.isLoading if you expose it)
            if (state.isLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}