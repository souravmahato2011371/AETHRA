package com.aethra.launcher.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: android.net.Uri
)

private enum class MusicTab { HOME, SEARCH, LIBRARY }

private val Ink = Color(0xFF101211)
private val Panel = Color(0xFF202622)
private val Panel2 = Color(0xFF303A32)
private val Paper = Color(0xFFF4F0E6)
private val Lime = Color(0xFFC5F36A)
private val Orange = Color(0xFFFF5938)
private val Muted = Color(0xFFA5ADA2)
private val Blue = Color(0xFF4A70FF)

@Composable
fun AethraMusicApp(controller: MediaController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var tab by remember { mutableStateOf(MusicTab.HOME) }
    var query by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()) }
    var currentArtist by remember { mutableStateOf(controller.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()) }
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    val prefs = remember { context.getSharedPreferences("aethra_music", android.content.Context.MODE_PRIVATE) }\n    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }\n    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    fun refreshLibrary() {
        if (!permissionGranted) return
        scope.launch {
            tracks = withContext(Dispatchers.IO) { queryDeviceTracks(context) }
        }
    }
}