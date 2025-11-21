package com.project.mobilevault.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.mobilevault.data.db.VaultEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    entries: List<VaultEntry>,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Your Vault") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Lock") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add") }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { pad ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad)) {
                Text(
                    text = "No entries yet. Tap + to add your first secret.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(entries) { e ->
                    ListItem(
                        headlineContent = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("Updated: ${e.updatedAt}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(e.id) }
                            .padding(horizontal = 8.dp)
                    )
                    Divider()
                }
            }
        }
    }
}