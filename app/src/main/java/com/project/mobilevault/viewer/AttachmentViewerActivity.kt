package com.project.mobilevault.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.project.mobilevault.ui.theme.MobileVaultTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class AttachmentViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val attId = intent.getLongExtra("attId", -1L)
        val mime = intent.getStringExtra("mime") ?: "application/octet-stream"
        val uri = Uri.parse("content://${packageName}.viewer/attachment/$attId")
        setContent {
            MobileVaultTheme {
                ViewerScreen(uri = uri, mime = mime) {
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerScreen(uri: Uri, mime: String, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Preview") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { pad ->
        val isImage = mime.startsWith("image/")
        val isVideo = mime.startsWith("video/")
        val isAudio = mime.startsWith("audio/")
        if (isImage) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isVideo || isAudio) {
            val ctx = LocalContext.current
            // Build a simple ExoPlayer instance to play content:// stream from our decrypting provider
            val player = ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
            DisposableEffect(Unit) {
                onDispose { player.release() }
            }
            AndroidView(
                factory = { context -> PlayerView(context).apply { this.player = player } },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback simple message
            Text("Unsupported file type", modifier = Modifier.fillMaxSize())
        }
    }
}