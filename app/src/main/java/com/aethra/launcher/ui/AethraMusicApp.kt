package com.aethra.launcher.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Track(val id: Long, val title: String, val artist: String, val album: String, val duration: Long, val uri: android.net.Uri)
private enum class Tab { HOME, SEARCH, LIBRARY }

private val Ink = Color(0xFF101211)
private val Panel = Color(0xFF202622)
private val Raised = Color(0xFF303A32)
private val Paper = Color(0xFFF4F0E6)
private val Lime = Color(0xFFC5F36A)
private val Orange = Color(0xFFFF5938)
private val Muted = Color(0xFFA5ADA2)
private val Blue = Color(0xFF4A70FF)

@Composable
fun AethraMusicApp(controller: MediaController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()) }
    var artist by remember { mutableStateOf(controller.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()) }
    var playing by remember { mutableStateOf(controller.isPlaying) }
    val prefs = remember { context.getSharedPreferences("aethra_music", Context.MODE_PRIVATE) }
    var liked by remember { mutableStateOf(prefs.getStringSet("liked", emptySet())?.toSet() ?: emptySet()) }
    val scope = rememberCoroutineScope()
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }

    fun refresh() {
        if (!allowed) return
        scope.launch { tracks = withContext(Dispatchers.IO) { readTracks(context) } }
    }
    LaunchedEffect(allowed) { if (allowed) refresh() }
    DisposableEffect(controller) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                title = item?.mediaMetadata?.title?.toString().orEmpty()
                artist = item?.mediaMetadata?.artist?.toString().orEmpty()
            }
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    val visible = when (tab) {
        Tab.HOME -> tracks
        Tab.SEARCH -> tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
        Tab.LIBRARY -> tracks.filter { liked.contains(it.id.toString()) }
    }

    fun play(track: Track) {
        val playlist = tracks.map { it.asMediaItem() }
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        controller.setMediaItems(playlist, index, 0L)
        controller.prepare()
        controller.play()
        title = track.title
        artist = track.artist
        tab = Tab.HOME
    }
    fun shuffle() {
        val shuffled = tracks.shuffled()
        if (shuffled.isNotEmpty()) {
            controller.setMediaItems(shuffled.map { it.asMediaItem() })
            controller.prepare()
            controller.play()
            title = shuffled.first().title
            artist = shuffled.first().artist
        }
    }
    fun toggleLike(track: Track) {
        val key = track.id.toString()
        val next = if (liked.contains(key)) liked - key else liked + key
        liked = next
        prefs.edit().putStringSet("liked", next).apply()
    }

    AethraTheme {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column(Modifier.background(Ink)) {
                    if (title.isNotBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp)).background(Panel).padding(start = 12.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Orange), contentAlignment = Alignment.Center) {
                                Text("♫", color = Ink, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(artist, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { if (controller.isPlaying) controller.pause() else controller.play() }) {
                                Text(if (playing) "Ⅱ" else "▶", color = Lime, fontSize = 19.sp)
                            }
                            TextButton(onClick = { controller.seekToNextMediaItem() }) { Text("≫", color = Paper, fontSize = 19.sp) }
                        }
                    }
                    NavigationBar(containerColor = Panel) {
                        NavigationBarItem(tab == Tab.HOME, { tab = Tab.HOME }, icon = { Text("⌂", fontSize = 21.sp) }, label = { Text("Home") }, colors = navColors())
                        NavigationBarItem(tab == Tab.SEARCH, { tab = Tab.SEARCH }, icon = { Text("⌕", fontSize = 22.sp) }, label = { Text("Search") }, colors = navColors())
                        NavigationBarItem(tab == Tab.LIBRARY, { tab = Tab.LIBRARY }, icon = { Text("▤", fontSize = 19.sp) }, label = { Text("Library") }, colors = navColors())
                    }
                }
            }
        ) { inset ->
            Column(Modifier.fillMaxSize().padding(inset).padding(horizontal = 19.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("A E T H R A", color = Paper, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("MUSIC WITHOUT LIMITS", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Orange), contentAlignment = Alignment.Center) {
                        Text("A", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                }

                if (!allowed) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Panel).padding(22.dp)) {
                        Text("YOUR MUSIC. YOUR DEVICE.", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("Give AETHRA permission to find audio files stored on this device. Your library stays local. No account is needed.", color = Paper, fontSize = 14.sp, lineHeight = 21.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { permissionRequest.launch(permission) }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                            Text("ALLOW MUSIC ACCESS", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    when (tab) {
                        Tab.HOME -> {
                            HeroPanel(tracks.size, ::shuffle)
                            SectionHeading("YOUR MUSIC", "ON THIS DEVICE")
                        }
                        Tab.SEARCH -> {
                            Text("Find your sound.", color = Paper, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                                placeholder = { Text("Songs, artists, albums", color = Muted) },
                                shape = RoundedCornerShape(17.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Paper, unfocusedTextColor = Paper, cursorColor = Lime,
                                    focusedBorderColor = Lime, unfocusedBorderColor = Raised,
                                    focusedContainerColor = Panel, unfocusedContainerColor = Panel
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            SectionHeading("SEARCH RESULTS", "${visible.size} TRACKS")
                        }
                        Tab.LIBRARY -> {
                            Text("Your library.", color = Paper, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Text("Your saved music, kept on this device.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(16.dp))
                            SectionHeading("LIKED MUSIC", "${visible.size} TRACKS")
                        }
                    }
                    if (visible.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp).clip(RoundedCornerShape(22.dp)).background(Panel).padding(21.dp)) {
                            Text(
                                when (tab) { Tab.LIBRARY -> "NOTHING SAVED YET"; Tab.SEARCH -> "NO MATCHES"; else -> "YOUR LIBRARY IS WAITING" },
                                color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (tab) {
                                    Tab.LIBRARY -> "Tap the heart beside a track to save it here."
                                    Tab.SEARCH -> "Try another song, artist or album."
                                    else -> "Add audio files to your device, then refresh your library."
                                }, color = Paper, fontSize = 13.sp, lineHeight = 19.sp
                            )
                            TextButton(onClick = ::refresh) { Text("REFRESH LIBRARY ↻", color = Lime, fontWeight = FontWeight.Bold) }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(visible, key = { it.id }) { track ->
                                TrackRow(track, liked.contains(track.id.toString()), { play(track) }, { toggleLike(track) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun readTracks(context: Context): List<Track> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION)
    val result = mutableListOf<Track>()
    context.contentResolver.query(collection, columns, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val key = cursor.getLong(id)
            result += Track(
                key, cursor.getString(title) ?: "Unknown track",
                cursor.getString(artist)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                cursor.getString(album) ?: "Unknown album", cursor.getLong(duration),
                ContentUris.withAppendedId(collection, key)
            )
        }
    }
    return result
}

private fun Track.asMediaItem() = MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()).build()

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Lime, selectedTextColor = Lime, unselectedIconColor = Muted,
    unselectedTextColor = Muted, indicatorColor = Raised
)

@Composable
private fun HeroPanel(count: Int, onShuffle: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(27.dp)).background(Paper)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Lime, size.minDimension * .43f, androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .04f))
            drawCircle(Orange, size.minDimension * .17f, androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .85f))
            drawRect(Blue, androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .46f), androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .11f))
        }
        Column(Modifier.fillMaxSize().padding(21.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("FIELD NOTE  /  001", color = Ink.copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Column {
                Text("Your sound.", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text("$count TRACKS ON THIS DEVICE", color = Ink.copy(alpha = .7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onShuffle, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) {
                    Text("SHUFFLE PLAY ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(9.dp))
                Text("NO ACCOUNT · NO AETHRA ADS", color = Ink.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SectionHeading(label: String, meta: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
        Text(meta, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
    }
}

@Composable
private fun TrackRow(track: Track, isLiked: Boolean, onPlay: () -> Unit, onLike: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable(onClick = onPlay).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).clip(RoundedCornerShape(11.dp)).background(if (isLiked) Orange else Raised), contentAlignment = Alignment.Center) {
            Text("♫", color = if (isLiked) Ink else Lime, fontSize = 22.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatTime(track.duration), color = Muted, fontSize = 10.sp)
        TextButton(onClick = onLike, contentPadding = PaddingValues(3.dp)) {
            Text(if (isLiked) "♥" else "♡", color = if (isLiked) Orange else Muted, fontSize = 21.sp)
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
