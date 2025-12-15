package com.project.mobilevault.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.mobilevault.data.db.VaultEntry
import androidx.compose.animation.core.animateFloatAsState
import android.provider.Settings
import com.google.accompanist.placeholder.material.placeholder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import android.net.Uri
import com.project.mobilevault.di.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultListScreen(
    entries: List<VaultEntry>,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val reduceMotion = remember {
        try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Throwable) { false }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Your Vault") },
                actions = {
                    var locking by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(targetValue = if (locking) 1.15f else 1f, label = "lockScale")
                    IconButton(
                        onClick = {
                            if (reduceMotion) {
                                onLogout(); return@IconButton
                            }
                            locking = true
                        },
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    ) {
                        Icon(
                            imageVector = if (locking) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (locking) "Lock" else "Unlock"
                        )
                    }
                    // Trigger the logout slightly after the icon animates
                    if (!reduceMotion) {
                        LaunchedEffect(key1 = locking) {
                            if (locking) {
                                kotlinx.coroutines.delay(180)
                                onLogout()
                                kotlinx.coroutines.delay(120)
                                locking = false
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add") }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { pad ->
        if (reduceMotion) {
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
                    items(entries, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("Updated: ${formatUpdated(e.updatedAt)}") },
                            leadingContent = { EntryThumbnail(entryId = e.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(e.id) }
                                .padding(horizontal = 8.dp)
                        )
                        Divider()
                    }
                }
            }
        } else {
            androidx.compose.animation.Crossfade(
                targetState = entries.isEmpty(),
                label = "vault_list_crossfade"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(Modifier.fillMaxSize().padding(pad)) {
                        Text(
                            text = "No entries yet. Tap + to add your first secret.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    Surface(tonalElevation = 2.dp, shadowElevation = 1.dp, modifier = Modifier.padding(pad)) {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(entries, key = { it.id }) { e ->
                                ListItem(
                                    headlineContent = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text("Updated: ${formatUpdated(e.updatedAt)}") },
                                    leadingContent = { EntryThumbnail(entryId = e.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement()
                                        .clickable { onOpen(e.id) }
                                        .padding(horizontal = 8.dp)
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatUpdated(ts: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
        sdf.format(java.util.Date(ts))
    } catch (_: Throwable) {
        ts.toString()
    }
}

@Composable
private fun EntryThumbnail(entryId: Long, size: Dp = 56.dp) {
    val ctx = LocalContext.current
    val preview by produceState<Pair<Uri, String>?>(initialValue = null, key1 = entryId) {
        val repo = com.project.mobilevault.di.ServiceLocator.attachmentRepo(ctx)
        val att = runCatching { repo.getLatestPreviewAttachmentForEntry(entryId) }.getOrNull()
        value = att?.let {
            val uri = Uri.parse("content://${ctx.packageName}.viewer/attachment/${it.id}")
            uri to it.mimeType
        }
    }
    val shape = RoundedCornerShape(8.dp)
    if (preview == null) {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .placeholder(visible = true)
        ) {}
    } else {
        val (uri, _) = preview!!
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
}