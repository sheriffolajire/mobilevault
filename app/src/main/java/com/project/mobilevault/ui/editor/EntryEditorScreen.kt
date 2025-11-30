package com.project.mobilevault.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.mobilevault.data.db.Attachment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    state: androidx.compose.runtime.State<EntryEditorViewModel.UiState>,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    onAddAttachment: ((String, String) -> Unit)? = null,
    onOpenAttachment: ((Attachment) -> Unit)? = null,
    onDeleteAttachment: ((Attachment) -> Unit)? = null,
) {
    var title by remember(state.value.title) { mutableStateOf(state.value.title) }
    var content by remember(state.value.content) { mutableStateOf(state.value.content) }

    val snackbarHostState = remember { SnackbarHostState() }
    val error = state.value.error
    LaunchedEffect(error) {
        if (error != null) snackbarHostState.showSnackbar(error)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entry") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") } },
                actions = { IconButton(onClick = { onSave(title, content) }) { Icon(Icons.Default.Check, contentDescription = "Save") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 8
            )

            // Attachments section
            if (onAddAttachment != null) {
                Text("Attachments", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { onAddAttachment(title, content) }) { Text("Add attachment") }
                Spacer(Modifier.height(8.dp))
                state.value.attachments.forEach { a ->
                    ListItem(
                        headlineContent = { Text(a.displayName) },
                        supportingContent = { Text("${a.mimeType} • ${a.sizeBytes} bytes") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (onOpenAttachment != null) TextButton(onClick = { onOpenAttachment(a) }) { Text("Open") }
                                if (onDeleteAttachment != null) TextButton(onClick = { onDeleteAttachment(a) }) { Text("Delete") }
                            }
                        }
                    )
                    Divider()
                }
            }

            Button(onClick = { onSave(title, content) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}