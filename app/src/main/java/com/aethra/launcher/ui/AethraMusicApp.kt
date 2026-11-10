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
    var favorites by remember { mutableStateOf(setOf<String>()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    fun refreshLibrary() {
        if (!permissionGranted) return
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val result = mutableListOf<Track>()
        resolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result += Track(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown track",
                    artist = cursor.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    duration = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(collection, id)
                )
            }
        }
        tracks = result
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) withContext(Dispatchers.IO) { refreshLibrary() }
    }

    DisposableEffect(controller) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTitle = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
                duration = controller.duration.coerceAtLeast(0L)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = controller.duration.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (true) {
            position = controller.currentPosition.coerceAtLeast(0L)
            duration = controller.duration.coerceAtLeast(0L)
            isPlaying = controller.isPlaying
            delay(500)
        }
    }

    fun playTrack(track: Track) {
        val items = tracks.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .build()
                )
                .build()
        }
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        controller.setMediaItems(items, index, 0L)
        controller.prepare()
        controller.play()
        currentTitle = track.title
        currentArtist = track.artist
        tab = MusicTab.HOME
    }

    val visibleTracks = when (tab) {
        MusicTab.HOME -> tracks
        MusicTab.SEARCH -> tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        MusicTab.LIBRARY -> tracks.filter { favorites.contains(it.id.toString()) }
    }

    AethraTheme {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column(Modifier.background(Ink)) {
                    if (currentTitle.isNotBlank()) {
                        MiniPlayer(
                            title = currentTitle,
                            artist = currentArtist,
                            playing = isPlaying,
                            onPlayPause = { if (controller.isPlaying) controller.pause() else controller.play() },
                            onNext = { controller.seekToNextMediaItem() },
                            onOpen = { tab = MusicTab.HOME }
                        )
                    }
                    NavigationBar(containerColor = Panel, contentColor = Paper) {
                        NavigationBarItem(
                            selected = tab == MusicTab.HOME,
                            onClick = { tab = MusicTab.HOME },
                            icon = { Text("⌂", fontSize = 21.sp) },
                            label = { Text("Home") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == MusicTab.SEARCH,
                            onClick = { tab = MusicTab.SEARCH },
                            icon = { Text("⌕", fontSize = 22.sp) },
                            label = { Text("Search") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == MusicTab.LIBRARY,
                            onClick = { tab = MusicTab.LIBRARY },
                            icon = { Text("▤", fontSize = 19.sp) },
                            label = { Text("Your library") },
                            colors = navColors()
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("A E T H R A", color = Paper, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("MUSIC WITHOUT LIMITS", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Orange), contentAlignment = Alignment.Center) {
                        Text("A", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                }

                if (!permissionGranted) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Panel).padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("YOUR MUSIC, IN ONE PLACE.", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Text("Allow AETHRA to read audio files on this device. Your library stays on your device; no account is needed.", color = Paper, fontSize = 14.sp, lineHeight = 21.sp)
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                                    else Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)
                        ) { Text("ALLOW MUSIC ACCESS", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    when (tab) {
                        MusicTab.HOME -> {
                            HeroPanel(
                                count = tracks.size,
                                onShuffle = {
                                    if (tracks.isNotEmpty()) {
                                        val shuffled = tracks.shuffled()
                                        val items = shuffled.map { trackItem(it) }
                                        controller.setMediaItems(items)
                                        controller.prepare()
                                        controller.play()
                                        currentTitle = shuffled.first().title
                                        currentArtist = shuffled.first().artist
                                    }
                                }
                            )
                            SectionHeading("YOUR MUSIC", "ON THIS DEVICE")
                        }
                        MusicTab.SEARCH -> {
                            Text("Find your sound.", color = Paper, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Songs, artists, albums", color = Muted) },
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Paper, unfocusedTextColor = Paper,
                                    focusedBorderColor = Lime, unfocusedBorderColor = Panel2,
                                    cursorColor = Lime, focusedContainerColor = Panel,
                                    unfocusedContainerColor = Panel
                                )
                            )
                            Spacer(Modifier.height(18.dp))
                            SectionHeading("SEARCH RESULTS", "${visibleTracks.size} TRACKS")
                        }
                        MusicTab.LIBRARY -> {
                            Text("Your library.", color = Paper, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Text("Saved tracks, stored locally.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(16.dp))
                            SectionHeading("LIKED MUSIC", "${visibleTracks.size} TRACKS")
                        }
                    }

                    if (visibleTracks.isEmpty()) {
                        EmptyMusicState(
                            title = when (tab) {
                                MusicTab.LIBRARY -> "NOTHING SAVED YET"
                                MusicTab.SEARCH -> "NO MATCHES"
                                else -> "YOUR LIBRARY IS WAITING"
                            },
                            detail = when (tab) {
                                MusicTab.LIBRARY -> "Tap the heart on any track to keep it here."
                                MusicTab.SEARCH -> "Try another title, artist or album."
                                else -> "Add audio files to your device, then refresh your library."
                            },
                            onRefresh = { refreshLibrary() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            items(visibleTracks, key = { it.id }) { track ->
                                TrackRow(
                                    track = track,
                                    liked = favorites.contains(track.id.toString()),
                                    onClick = { playTrack(track) },
                                    onLike = {
                                        val key = track.id.toString()
                                        favorites = if (favorites.contains(key)) favorites - key else favorites + key
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Lime,
    selectedTextColor = Lime,
    unselectedIconColor = Muted,
    unselectedTextColor = Muted,
    indicatorColor = Panel2
)

private fun trackItem(track: Track) = MediaItem.Builder()
    .setMediaId(track.id.toString())
    .setUri(track.uri)
    .setMediaMetadata(
        MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).build()
    ).build()

@Composable
private fun HeroPanel(count: Int, onShuffle: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(28.dp)).background(Paper)
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Lime, size.minDimension * .43f, androidx.compose.ui.geometry.Offset(size.width * .93f, size.height * .05f))
            drawCircle(Orange, size.minDimension * .17f, androidx.compose.ui.geometry.Offset(size.width * .77f, size.height * .86f))
            drawRect(Blue, topLeft = androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .46f), size = androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .11f))
        }
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("FIELD NOTE  /  001", color = Ink.copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Column {
                Text("Your sound.", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text("$count TRACKS ON THIS DEVICE", color = Ink.copy(alpha = .7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onShuffle, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) {
                    Text("SHUFFLE PLAY  ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text("NO ACCOUNT. NO AETHRA ADS.", color = Ink.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun SectionHeading(title: String, meta: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp, modifier = Modifier.weight(1f))
        Text(meta, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun TrackRow(track: Track, liked: Boolean, onClick: () -> Unit, onLike: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).clickable(onClick = onClick).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(if (liked) Orange else Panel2), contentAlignment = Alignment.Center) {
            Text("♫", color = if (liked) Ink else Lime, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(track.duration), color = Muted, fontSize = 10.sp)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onLike, contentPadding = PaddingValues(4.dp)) {
            Text(if (liked) "♥" else "♡", color = if (liked) Orange else Muted, fontSize = 21.sp)
        }
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    artist: String,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).clickable(onClick = onOpen).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(37.dp).clip(RoundedCornerShape(10.dp)).background(Orange), contentAlignment = Alignment.Center) {
                Text("♫", color = Ink, fontSize = 19.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onPlayPause) { Text(if (playing) "Ⅱ" else "▶", color = Lime, fontSize = 19.sp) }
            TextButton(onClick = onNext) { Text("≫", color = Paper, fontSize = 19.sp) }
        }
    }
}

@Composable
private fun EmptyMusicState(title: String, detail: String, onRefresh: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 30.dp).clip(RoundedCornerShape(23.dp)).background(Panel).padding(22.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = Paper, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRefresh) { Text("REFRESH LIBRARY  ↻", color = Lime, fontWeight = FontWeight.Bold) }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}