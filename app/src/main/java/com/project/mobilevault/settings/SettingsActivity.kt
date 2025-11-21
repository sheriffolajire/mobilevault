
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.project.mobilevault.repo.AuthPrefs
import com.project.mobilevault.ui.theme.MobileVaultTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MobileVaultTheme { SettingsScreen(onBack = { finish() }) } }
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

    val timeoutOptions = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RowWithSwitch(
                title = "Biometric quick unlock",
                checked = biometrics,
                onCheckedChange = { v ->
                    biometrics = v
                    settings.biometricsEnabled = v
                    if (!v) authPrefs.clearBiometricWrappedDek()
                }
            )

            DropdownSetting(
                label = "Idle timeout",
                valueMs = idle,
                options = timeoutOptions,
                onSelect = { ms -> idle = ms; settings.idleTimeoutMs = ms }
            )
            DropdownSetting(
                label = "Stillness timeout",
                valueMs = still,
                options = timeoutOptions,
                onSelect = { ms -> still = ms; settings.stillnessMs = ms }
            )
            DropdownSetting(
                label = "Background grace",
                valueMs = grace,
                options = listOf(0L, 5_000L, 10_000L, 15_000L, 30_000L, 60_000L),
                onSelect = { ms -> grace = ms; settings.bgGraceMs = ms }
            )
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