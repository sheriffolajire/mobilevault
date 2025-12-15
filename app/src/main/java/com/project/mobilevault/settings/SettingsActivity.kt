package com.project.mobilevault.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.project.mobilevault.repo.AuthPrefs
import com.project.mobilevault.ui.theme.MobileVaultTheme
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MobileVaultTheme { SettingsScreen(onBack = {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right)
        }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { SettingsPrefs(ctx) }
    val authPrefs = remember { AuthPrefs(ctx) }

    var biometrics by remember { mutableStateOf(settings.biometricsEnabled) }
    var idle by remember { mutableStateOf(settings.idleTimeoutMs) }
    var still by remember { mutableStateOf(settings.stillnessMs) }
    var grace by remember { mutableStateOf(settings.bgGraceMs) }
    var autoDeleteOnImport by remember { mutableStateOf(settings.autoDeleteOnImport) }

    val timeoutOptions = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RowWithSwitch(
                title = "Biometric quick unlock",
                checked = biometrics,
                onCheckedChange = { v ->
                    biometrics = v
                    settings.biometricsEnabled = v
                    if (!v) authPrefs.clearBiometricWrappedDek()
                }
            )

            // Auto-wipe policy
            var autoWipe by remember { mutableStateOf(settings.autoWipeEnabled) }
            var threshold by remember { mutableStateOf(settings.wipeThreshold) }
            RowWithSwitch(
                title = "Auto-wipe on failed attempts",
                checked = autoWipe,
                onCheckedChange = { v -> autoWipe = v; settings.autoWipeEnabled = v }
            )
            if (autoWipe) {
                ThresholdSetting(
                    label = "Wipe threshold",
                    value = threshold,
                    options = listOf(3, 5, 10, 15, 20),
                    onSelect = { t -> threshold = t; settings.wipeThreshold = t }
                )
            }

            SliderSetting(
                label = "Idle timeout",
                valueMs = idle,
                minMs = 15_000L,
                maxMs = 600_000L,
                stepMs = 5_000L,
                onChange = { ms -> idle = ms; settings.idleTimeoutMs = ms }
            )
            SliderSetting(
                label = "Stillness timeout",
                valueMs = still,
                minMs = 5_000L,
                maxMs = 180_000L,
                stepMs = 5_000L,
                onChange = { ms -> still = ms; settings.stillnessMs = ms }
            )
            SliderSetting(
                label = "Background grace",
                valueMs = grace,
                minMs = 0L,
                maxMs = 60_000L,
                stepMs = 5_000L,
                onChange = { ms -> grace = ms; settings.bgGraceMs = ms }
            )

            // Auto-delete originals option
            RowWithSwitch(
                title = "Auto-delete originals on import",
                checked = autoDeleteOnImport,
                onCheckedChange = { v -> autoDeleteOnImport = v; settings.autoDeleteOnImport = v }
            )

            Divider()
            // Backup & Restore
            BackupRestoreSection()

            Divider()
            // Change Password
            ChangePasswordSection()
        }
    }
}

@Composable
private fun RowWithSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    RowItem {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownSetting(label: String, valueMs: Long, options: List<Long>, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "$label: ${valueMs / 1000}s",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { ms ->
                DropdownMenuItem(text = { Text("${ms / 1000}s") }, onClick = { onSelect(ms); expanded = false })
            }
        }
    }
}

@Composable
private fun RowItem(content: @Composable RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}


@Composable
fun PasswordPromptDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Password") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThresholdSetting(label: String, value: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "$label: $value",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt.toString()) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
private fun BackupRestoreSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBackup by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var backupPass by remember { mutableStateOf<CharArray?>(null) }
    var restorePass by remember { mutableStateOf<CharArray?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: android.net.Uri? ->
        val pass = backupPass
        if (pass != null && uri != null) {
            Thread { com.project.mobilevault.backup.Exporter.exportAll(ctx, uri, pass) }.start()
        }
        backupPass = null
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        val pass = restorePass
        if (pass != null && uri != null) {
            Thread { com.project.mobilevault.backup.Importer.importAll(ctx, uri, pass) }.start()
        }
        restorePass = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { showBackup = true }, modifier = Modifier.fillMaxWidth()) { Text("Export encrypted backup") }
        OutlinedButton(onClick = { showRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Import encrypted backup") }
    }

    if (showBackup) PasswordPromptDialog(
        title = "Set backup password",
        onDismiss = { showBackup = false },
        onConfirm = { pw ->
            backupPass = pw.toCharArray()
            showBackup = false
            exportLauncher.launch("mobile_vault_backup.mvbak")
        }
    )
    if (showRestore) PasswordPromptDialog(
        title = "Enter backup password",
        onDismiss = { showRestore = false },
        onConfirm = { pw ->
            restorePass = pw.toCharArray()
            showRestore = false
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }
    )
}

@Composable
private fun ChangePasswordSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Change master password") }

    if (show) {
        var current by remember { mutableStateOf("") }
        var next by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Change master password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Current password") })
                    OutlinedTextField(value = next, onValueChange = { next = it }, label = { Text("New password") })
                    OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm password") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (next.isNotBlank() && next == confirm) {
                        scope.launch {
                            val mgr = com.project.mobilevault.security.PasswordChangeManager(com.project.mobilevault.data.db.AppDb.get(ctx).authDao())
                            val ok = mgr.changePassword(current.toCharArray(), next.toCharArray())
                            show = false
                        }
                    }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        )
    }
}


@Composable
private fun SliderSetting(
    label: String,
    valueMs: Long,
    minMs: Long,
    maxMs: Long,
    stepMs: Long,
    onChange: (Long) -> Unit
) {
    var slider by remember { mutableStateOf(valueMs.toFloat()) }
    LaunchedEffect(valueMs) { slider = valueMs.toFloat() }

    val minF = minMs.toFloat()
    val maxF = maxMs.toFloat()
    val stepF = stepMs.toFloat()
    val stepsCount = (((maxMs - minMs) / stepMs).toInt() - 1).coerceAtLeast(0)

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(minMs/1000)}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(slider.toLong()/1000)}s", style = MaterialTheme.typography.labelMedium)
                    Text("${(maxMs/1000)}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = slider.coerceIn(minF, maxF),
                    onValueChange = { slider = it },
                    valueRange = minF..maxF,
                    steps = stepsCount,
                    onValueChangeFinished = {
                        val snapped = (((slider - minF) / stepF).roundToInt() * stepF + minF).coerceIn(minF, maxF)
                        onChange(snapped.toLong())
                    }
                )
            }
        }
    )
}