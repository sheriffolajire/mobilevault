package com.project.mobilevault.ui.editor

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.material.placeholder
import com.project.mobilevault.data.db.Attachment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentThumbnail(att: Attachment, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    val uri = remember(att.id) { Uri.parse("content://${ctx.packageName}.viewer/attachment/${att.id}") }
    val req = coil.request.ImageRequest.Builder(ctx)
        .data(uri)
        .crossfade(true)
        .build()
    AsyncImage(
        model = req,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(shape)
    )
}

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
            if (state.value.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            // Details card with shimmer placeholders while loading
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.value.isLoading) {
                        Box(Modifier.fillMaxWidth().height(56.dp).placeholder(true))
                    } else {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.value.isLoading) {
                        Box(Modifier.fillMaxWidth().height(160.dp).placeholder(true))
                    } else {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Content") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 6
                        )
                    }
                }
            }

            // Attachments section with animated reveal
            if (onAddAttachment != null) {
                AnimatedVisibility(visible = true, label = "attachments") {
                    Column {
                        Text("Attachments", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { onAddAttachment(title, content) }) { Text("Add attachment") }
                        Spacer(Modifier.height(8.dp))
                        state.value.attachments.forEach { a ->
                            ListItem(
                                leadingContent = { AttachmentThumbnail(a) },
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
                }
            }

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "savePress")
            Button(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank(),
                interactionSource = interaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
                Text("Save")
            }
        }
    }
}