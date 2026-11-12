package com.aethra.launcher.ui

import androidx.compose.ui.layout.onSizeChanged

import com.skydoves.cloudy.sky

import com.skydoves.cloudy.rememberSky

import com.skydoves.cloudy.liquidGlass

import com.skydoves.cloudy.cloudy

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.aethra.launcher.data.YouTubeDataCatalog
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: android.net.Uri,
    val source: String = "LOCAL",
    val artworkUrl: String? = null,
    val shareUrl: String? = null,
    val licenseUrl: String? = null
) {
    val stableKey: String get() = "$source:$id"
}

private fun com.aethra.launcher.data.OnlineTrack.asAppTrack(): Track = Track(
    id = id.toLongOrNull() ?: id.hashCode().toLong(),
    title = title,
    artist = artist,
    album = album,
    duration = durationSeconds * 1000L,
    uri = android.net.Uri.parse(watchUrl),
    source = "YOUTUBE_VIDEO",
    artworkUrl = artworkUrl,
    shareUrl = shareUrl,
    licenseUrl = licenseUrl
)

private enum class AethraTab { HOME, SEARCH, LIBRARY, SETTINGS }

private val artworkCache = object : LruCache<String, Bitmap>(48) {}

@Composable
fun AethraMusicApp(controller: MediaController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("aethra_music", Context.MODE_PRIVATE) }

    var themeMode by remember {
        mutableStateOf(if (prefs.getString("theme_mode", "DARK") == "LIGHT") ThemeMode.LIGHT else ThemeMode.DARK)
    }
    var visualStyle by remember {
        mutableStateOf(readVisualStyle(prefs.getString("visual_style", null)))
    }
    var setupComplete by remember { mutableStateOf(true) }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var audioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var onlineTracks by remember { mutableStateOf(emptyList<Track>()) }
    var onlineCatalogLoading by remember { mutableStateOf(false) }
    var onlineCatalogError by remember { mutableStateOf("") }
    var playbackQueue by remember { mutableStateOf(emptyList<Track>()) }
    var queueIndex by remember { mutableIntStateOf(-1) }
    var activeTab by remember { mutableStateOf(AethraTab.HOME) }
    var query by remember { mutableStateOf("") }
    var liked by remember {
        mutableStateOf(prefs.getStringSet("liked", emptySet())?.toSet() ?: emptySet())
    }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var currentTitle by remember {
        mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty())
    }
    var currentArtist by remember {
        mutableStateOf(controller.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty())
    }
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermission = granted
    }

    fun saveAppearance(mode: ThemeMode, style: VisualStyle) {
        themeMode = mode
        visualStyle = style
        prefs.edit()
            .putString("theme_mode", mode.name)
            .putString("visual_style", style.name)
            .apply()
    }

    fun finishSetup(mode: ThemeMode, style: VisualStyle) {
        saveAppearance(mode, style)
        setupComplete = true
        prefs.edit().putBoolean("setup_complete", true).apply()
    }

    LaunchedEffect(audioPermission) {
        if (audioPermission) {
            tracks = withContext(Dispatchers.IO) { readTracks(context) }
        }
    }

    LaunchedEffect(Unit) {
        onlineCatalogLoading = true
        try {
            onlineTracks = YouTubeDataCatalog.search("").map { it.asAppTrack() }
            onlineCatalogError = ""
        } catch (error: Exception) {
            onlineCatalogError = error.message ?: "Online discovery is temporarily unavailable."
        } finally {
            onlineCatalogLoading = false
        }
    }

    DisposableEffect(controller, tracks, onlineTracks, playbackQueue) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                currentTitle = item?.mediaMetadata?.title?.toString().orEmpty()
                currentArtist = item?.mediaMetadata?.artist?.toString().orEmpty()
                queueIndex = controller.currentMediaItemIndex
                currentTrack = playbackQueue.firstOrNull { it.stableKey == item?.mediaId }
                    ?: (tracks + onlineTracks).firstOrNull { it.stableKey == item?.mediaId }
            }

            override fun onIsPlayingChanged(isNowPlaying: Boolean) {
                isPlaying = isNowPlaying
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (true) {
            isPlaying = controller.isPlaying
            positionMs = controller.currentPosition.coerceAtLeast(0L)
            val playerDuration = controller.duration
            durationMs = if (playerDuration != C.TIME_UNSET && playerDuration > 0L) {
                playerDuration
            } else {
                currentTrack?.duration ?: 0L
            }
            delay(300)
        }
    }

    fun playTrack(track: Track) {
        if (track.source == "YOUTUBE_VIDEO") {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, track.uri)
            runCatching { context.startActivity(intent) }
            return
        }
        val queue = tracks.distinctBy { it.stableKey }
        val selectedQueue = if (queue.any { it.stableKey == track.stableKey }) queue else listOf(track)
        val index = selectedQueue.indexOfFirst { it.stableKey == track.stableKey }.coerceAtLeast(0)
        playbackQueue = selectedQueue
        queueIndex = index
        controller.setMediaItems(selectedQueue.map { it.asMediaItem() }, index, 0L)
        controller.prepare()
        controller.play()
        currentTrack = track
        currentTitle = track.title
        currentArtist = track.artist
        nowPlayingOpen = false
    }

    fun shuffleTracks() {
        val shuffled = tracks.distinctBy { it.stableKey }.shuffled()
        if (shuffled.isNotEmpty()) {
            playbackQueue = shuffled
            queueIndex = 0
            controller.setMediaItems(shuffled.map { it.asMediaItem() })
            controller.prepare()
            controller.play()
            currentTrack = shuffled.first()
            currentTitle = shuffled.first().title
            currentArtist = shuffled.first().artist
        }
    }

    fun toggleLike(track: Track) {
        val key = track.stableKey
        val oldKey = track.id.toString()
        val alreadyLiked = liked.contains(key) || (track.source == "LOCAL" && liked.contains(oldKey))
        val next = if (alreadyLiked) liked - key - oldKey else liked + key
        liked = next
        prefs.edit().putStringSet("liked", next).apply()
    }

    AethraTheme(style = visualStyle, mode = themeMode) {
        if (!setupComplete) {
            FirstRunSetup(
                mode = themeMode,
                style = visualStyle,
                onModeChange = { saveAppearance(it, visualStyle) },
                onStyleChange = { saveAppearance(themeMode, it) },
                onContinue = { finishSetup(themeMode, visualStyle) }
            )
        } else {
            AethraExperience(
                controller = controller,
                tracks = tracks,
                onlineTracks = onlineTracks,
                onOnlineResults = { onlineTracks = it },
                onlineCatalogLoading = onlineCatalogLoading,
                onlineCatalogError = onlineCatalogError,
                audioPermission = audioPermission,
                requestAudioPermission = { permissionRequest.launch(permission) },
                activeTab = activeTab,
                onTabChange = { activeTab = it },
                query = query,
                onQueryChange = { query = it },
                liked = liked,
                onToggleLike = ::toggleLike,
                currentTrack = currentTrack,
                activeTrackKey = currentTrack?.stableKey,
                currentTitle = currentTitle,
                currentArtist = currentArtist,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                playbackQueue = playbackQueue,
                queueIndex = queueIndex,
                onPlayQueueItem = { index ->
                    if (index in playbackQueue.indices) {
                        queueIndex = index
                        controller.seekToDefaultPosition(index)
                        controller.play()
                    }
                },
                onPlayTrack = ::playTrack,
                onShuffle = ::shuffleTracks,
                onPlayPause = { if (controller.isPlaying) controller.pause() else controller.play() },
                onNext = { controller.seekToNextMediaItem() },
                onPrevious = { controller.seekToPreviousMediaItem() },
                onSeek = { controller.seekTo(it) },
                nowPlayingOpen = nowPlayingOpen,
                onOpenNowPlaying = { nowPlayingOpen = true },
                onCloseNowPlaying = { nowPlayingOpen = false },
                mode = themeMode,
                style = visualStyle,
                onModeChange = { saveAppearance(it, visualStyle) },
                onStyleChange = { saveAppearance(themeMode, it) }
            )
        }
    }
}

@Composable
private fun FirstRunSetup(
    mode: ThemeMode,
    style: VisualStyle,
    onModeChange: (ThemeMode) -> Unit,
    onStyleChange: (VisualStyle) -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalAethraColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "A E T H R A  /  WELCOME",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.4.sp
        )
        Text(
            "Make it sound like you.",
            color = colors.text,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 35.sp,
            lineHeight = 40.sp
        )
        Text(
            "Choose your atmosphere. You can change both choices any time in Settings.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        SetupSectionTitle("01  /  APPEARANCE", "Choose a light or dark canvas")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = "Light",
                selected = mode == ThemeMode.LIGHT,
                onClick = { onModeChange(ThemeMode.LIGHT) },
                icon = { Icon(Icons.Rounded.LightMode, contentDescription = null) }
            )
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = "Dark",
                selected = mode == ThemeMode.DARK,
                onClick = { onModeChange(ThemeMode.DARK) },
                icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) }
            )
        }

        SetupSectionTitle("02  /  DESIGN LANGUAGE", "Three distinct AETHRA experiences")
        VisualStyle.entries.forEach { option ->
            StyleChoice(
                style = option,
                selected = style == option,
                onClick = { onStyleChange(option) }
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) 12.dp else 20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.background
            )
        ) {
            Text("ENTER AETHRA", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        }
        Text(
            "NO ACCOUNT REQUIRED  ·  NO AETHRA ADS",
            modifier = Modifier.fillMaxWidth(),
            color = colors.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun SetupSectionTitle(title: String, subtitle: String) {
    val colors = LocalAethraColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
        Text(subtitle, color = colors.muted, fontSize = 12.sp)
    }
}

@Composable
private fun ThemeChoice(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val colors = LocalAethraColors.current
    val borderColor by animateColorAsState(
        if (selected) colors.accent else colors.outline,
        animationSpec = spring(stiffness = 420f),
        label = "theme-choice-border"
    )
    Card(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.accent.copy(alpha = 0.13f) else colors.surface
        )
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.accent) { icon() }
            Text(title, color = colors.text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(colors.accent))
            }
        }
    }
}

@Composable
private fun StyleChoice(
    style: VisualStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAethraColors.current
    val borderColor by animateColorAsState(
        if (selected) colors.accent else colors.outline,
        animationSpec = spring(stiffness = 430f),
        label = "style-choice-border"
    )
    val preview = when (style) {
        VisualStyle.LIQUID_GLASS -> listOf(Color(0xFF273D68), Color(0xFF7E9DCF), Color(0xFF78D5D3))
        VisualStyle.NEO_CONSTRUCTIVIST_POP -> listOf(Color(0xFF161B18), Color(0xFFFF5B37), Color(0xFFC5F36A))
        VisualStyle.SIGNATURE_PREMIUM -> listOf(Color(0xFF14171B), Color(0xFFD8BD83), Color(0xFFE9E2D4))
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) 3.dp else 22.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(width = 84.dp, height = 76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(preview))
            ) {
                when (style) {
                    VisualStyle.LIQUID_GLASS -> {
                        Box(Modifier.size(48.dp).offset(x = 30.dp, y = 12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f)))
                        Box(Modifier.size(35.dp).offset(x = 8.dp, y = 30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
                    }
                    VisualStyle.NEO_CONSTRUCTIVIST_POP -> {
                        Box(Modifier.size(28.dp).offset(x = 44.dp, y = 9.dp).background(Color(0xFFC5F36A)))
                        Box(Modifier.width(56.dp).height(10.dp).offset(x = 10.dp, y = 54.dp).background(Color(0xFF4A70FF)))
                    }
                    VisualStyle.SIGNATURE_PREMIUM -> {
                        Box(Modifier.fillMaxWidth().height(1.dp).offset(y = 24.dp).background(Color(0xFFD8BD83)))
                        Box(Modifier.size(28.dp).offset(x = 28.dp, y = 25.dp).clip(CircleShape).border(1.dp, Color(0xFFD8BD83), CircleShape))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(style.title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(style.subtitle, color = colors.muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            if (selected) {
                Icon(Icons.Rounded.Favorite, contentDescription = "Selected style", tint = colors.accent)
            }
        }
    }
}

@Composable
private fun AethraExperience(
    controller: MediaController,
    tracks: List<Track>,
    onlineTracks: List<Track>,
    onOnlineResults: (List<Track>) -> Unit,
    onlineCatalogLoading: Boolean,
    onlineCatalogError: String,
    audioPermission: Boolean,
    requestAudioPermission: () -> Unit,
    activeTab: AethraTab,
    onTabChange: (AethraTab) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    liked: Set<String>,
    onToggleLike: (Track) -> Unit,
    currentTrack: Track?,
    activeTrackKey: String?,
    currentTitle: String,
    currentArtist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playbackQueue: List<Track>,
    queueIndex: Int,
    onPlayQueueItem: (Int) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onShuffle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    nowPlayingOpen: Boolean,
    onOpenNowPlaying: () -> Unit,
    onCloseNowPlaying: () -> Unit,
    mode: ThemeMode,
    style: VisualStyle,
    onModeChange: (ThemeMode) -> Unit,
    onStyleChange: (VisualStyle) -> Unit
) {
    val colors = LocalAethraColors.current
    val liquidSky = rememberSky()
    val visibleTracks = when (activeTab) {
        AethraTab.HOME -> tracks
        AethraTab.SEARCH -> tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        AethraTab.LIBRARY -> (tracks + onlineTracks).filter {
            liked.contains(it.stableKey) || (it.source == "LOCAL" && liked.contains(it.id.toString()))
        }
        AethraTab.SETTINGS -> emptyList()
    }

    AethraVisualBackdrop(style = style, colors = colors) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                val navigationGlassShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                Column(
                    Modifier.fillMaxWidth().then(
                        if (colors.glass) {
                            Modifier
                                .clip(navigationGlassShape)
                                .cloudy(
                                    sky = liquidSky,
                                    radius = 10,
                                    tint = Color(0xFFB9D8FF).copy(alpha = 0.09f),
                                    shape = navigationGlassShape
                                )
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color(0xFF8FB9F4).copy(alpha = 0.045f), Color(0xFF17294C).copy(alpha = 0.22f))))
                                .border(1.dp, Color.White.copy(alpha = 0.34f), navigationGlassShape)
                        } else {
                            Modifier.background(colors.background)
                        }
                    )
                ) {
                    AnimatedVisibility(
                        visible = currentTitle.isNotBlank(),
                        enter = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(240)),
                        exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(180))
                    ) {
                        MiniPlayer(
                            track = currentTrack,
                            title = currentTitle,
                            artist = currentArtist,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onOpen = onOpenNowPlaying,
                            onPlayPause = onPlayPause,
                            onNext = onNext
                        )
                    }
                    NavigationBar(
                        containerColor = if (colors.glass) Color.Transparent else colors.surface,
                        tonalElevation = 0.dp
                    ) {
                        val destinations = listOf(
                            Triple(AethraTab.HOME, "Home", Icons.Rounded.Home),
                            Triple(AethraTab.SEARCH, "Search", Icons.Rounded.Search),
                            Triple(AethraTab.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
                            Triple(AethraTab.SETTINGS, "Settings", Icons.Rounded.Settings)
                        )
                        destinations.forEach { (tab, label, icon) ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { onTabChange(tab) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.accent,
                                    selectedTextColor = colors.accent,
                                    unselectedIconColor = colors.muted,
                                    unselectedTextColor = colors.muted,
                                    indicatorColor = colors.accent.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }
            }
        ) { insets ->
            AnimatedContent(
                targetState = activeTab,
                modifier = Modifier.fillMaxSize().padding(insets).then(if (colors.glass) Modifier.sky(liquidSky) else Modifier),
                transitionSpec = {
                    (fadeIn(tween(230)) + slideInHorizontally(tween(230)) { it / 18 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 24 })
                },
                label = "aethra-navigation"
            ) { page ->
                when (page) {
                    AethraTab.HOME -> HomeScreen(
                        tracks = tracks,
                        onlineTracks = onlineTracks,
                        activeTrackKey = activeTrackKey,
                        onlineLoading = onlineCatalogLoading,
                        onlineError = onlineCatalogError,
                        audioPermission = audioPermission,
                        requestAudioPermission = requestAudioPermission,
                        liked = liked,
                        onPlayTrack = onPlayTrack,
                        onToggleLike = onToggleLike,
                        onShuffle = onShuffle
                    )
                    AethraTab.SEARCH -> SearchScreen(
                        query = query,
                        onQueryChange = onQueryChange,
                        tracks = visibleTracks,
                        onlineTracks = onlineTracks,
                        activeTrackKey = activeTrackKey,
                        onOnlineResults = onOnlineResults,
                        liked = liked,
                        onPlayTrack = onPlayTrack,
                        onToggleLike = onToggleLike
                    )
                    AethraTab.LIBRARY -> LibraryScreen(
                        tracks = visibleTracks,
                        activeTrackKey = activeTrackKey,
                        likedCount = liked.size,
                        onPlayTrack = onPlayTrack,
                        onToggleLike = onToggleLike
                    )
                    AethraTab.SETTINGS -> SettingsScreen(
                        mode = mode,
                        style = style,
                        onModeChange = onModeChange,
                        onStyleChange = onStyleChange
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = nowPlayingOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(240)) + slideInVertically(spring(dampingRatio = 0.82f, stiffness = 360f)) { it / 3 },
            exit = fadeOut(tween(170)) + slideOutVertically(tween(180)) { it / 2 }
        ) {
            NowPlayingScreen(
                track = currentTrack,
                title = currentTitle,
                artist = currentArtist,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                queue = playbackQueue,
                queueIndex = queueIndex,
                onPlayQueueItem = onPlayQueueItem,
                onClose = onCloseNowPlaying,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek
            )
        }
    }
}

@Composable
private fun HomeScreen(
    tracks: List<Track>,
    onlineTracks: List<Track>,
    activeTrackKey: String?,
    onlineLoading: Boolean,
    onlineError: String,
    audioPermission: Boolean,
    requestAudioPermission: () -> Unit,
    liked: Set<String>,
    onPlayTrack: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onShuffle: () -> Unit
) {
    val colors = LocalAethraColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("A E T H R A", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("MUSIC WITHOUT LIMITS", color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = colors.background, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("YOUR SOUND, YOUR SPACE.", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text("Find your next\nfavourite.", color = colors.text, style = MaterialTheme.typography.headlineLarge, fontSize = 34.sp, lineHeight = 38.sp)
        }

        HeroPanel(count = tracks.size, onShuffle = onShuffle)

        if (!audioPermission) {
            AethraCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR LIBRARY IS READY WHEN YOU ARE", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                    Text("Allow AETHRA to scan audio stored on this device. This permission is only used to show and play your local tracks.", color = colors.text, fontSize = 13.sp, lineHeight = 19.sp)
                    Button(onClick = requestAudioPermission, colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)) {
                        Text("ALLOW MUSIC ACCESS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (tracks.isEmpty()) {
            AethraCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR LOCAL LIBRARY IS EMPTY", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Discover music videos online, or play audio stored on this device.", color = colors.text, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        } else {
            SectionHeading("ON THIS DEVICE", tracks.size.toString() + " TRACKS")
            tracks.take(8).forEach { track ->
                TrackRow(
                    track = track,
                    isLiked = liked.contains(track.id.toString()),
                    onPlay = { onPlayTrack(track) },
                    onLike = { onToggleLike(track) },
                    isPlaying = activeTrackKey == track.stableKey
                )
            }
        }

        SectionHeading("DISCOVER ONLINE", if (onlineLoading) "LOADING" else onlineTracks.size.toString() + " TRACKS")
        if (onlineError.isNotBlank()) {
            Text(onlineError, color = colors.muted, fontSize = 12.sp)
        } else if (onlineLoading && onlineTracks.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                Text("Finding music on YouTube…", color = colors.muted, fontSize = 12.sp)
            }
        } else if (onlineTracks.isEmpty()) {
            Text("No online tracks are available right now. Try Discover again later.", color = colors.muted, fontSize = 12.sp)
        } else {
            onlineTracks.take(8).forEach { track ->
                TrackRow(
                    track = track,
                    isLiked = liked.contains(track.stableKey),
                    onPlay = { onPlayTrack(track) },
                    onLike = { onToggleLike(track) },
                    isPlaying = activeTrackKey == track.stableKey
                )
            }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    tracks: List<Track>,
    onlineTracks: List<Track>,
    activeTrackKey: String?,
    onOnlineResults: (List<Track>) -> Unit,
    liked: Set<String>,
    onPlayTrack: (Track) -> Unit,
    onToggleLike: (Track) -> Unit
) {
    val colors = LocalAethraColors.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var onlineError by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }

    fun searchOnline(term: String) {
        scope.launch {
            loading = true
            onlineError = ""
            try {
                onOnlineResults(YouTubeDataCatalog.search(term).map { it.asAppTrack() })
                hasSearched = true
            } catch (error: Exception) {
                onlineError = error.message ?: "Could not reach the online music catalogue."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasSearched && onlineTracks.isEmpty()) searchOnline("")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Discover", color = colors.text, style = MaterialTheme.typography.headlineLarge, fontSize = 34.sp)
        Text("Find tracks, artists and music videos on YouTube. Choose a result to open it on YouTube.", color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Track, artist, album or genre", color = colors.muted) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.muted) },
            shape = RoundedCornerShape(if (colors.sharp) 3.dp else if (colors.glass) 24.dp else 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.accent,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.outline,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )
        Button(
            onClick = { searchOnline(query) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(if (colors.sharp) 3.dp else 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = if (colors.sharp) Color.White else colors.background)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = if (colors.sharp) Color.White else colors.background)
                Spacer(Modifier.width(10.dp))
                Text("SEARCHING…")
            } else Text("SEARCH ONLINE MUSIC", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
        }
        if (onlineError.isNotBlank()) {
            AethraCard(Modifier.fillMaxWidth()) {
                Text(onlineError, modifier = Modifier.padding(16.dp), color = colors.accentSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            Text("YOUTUBE · VIDEO DISCOVERY", color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        val unifiedResults = (tracks + onlineTracks).distinctBy { it.stableKey }
        SectionHeading(
            if (query.isBlank()) "YOUR MUSIC & OPEN DISCOVERY" else "ALL MATCHING MUSIC",
            unifiedResults.size.toString() + " TRACKS"
        )
        if (unifiedResults.isEmpty()) {
            if (hasSearched && !loading && onlineError.isBlank()) {
                EmptyState("NO MATCHES FOUND", "Try another title, artist or genre. Local tracks are matched instantly; online results come from YouTube.")
            } else if (tracks.isEmpty()) {
                Text("Search for a song or artist on YouTube. Music stored on this device will appear here too.", color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            unifiedResults.take(48).forEach { track ->
                TrackRow(
                    track,
                    liked.contains(track.stableKey) || (track.source == "LOCAL" && liked.contains(track.id.toString())),
                    { onPlayTrack(track) },
                    { onToggleLike(track) },
                    activeTrackKey == track.stableKey
                )
            }
        }
        Text(
            "YouTube results open in YouTube. They are discovery links, not playable audio streams inside AETHRA.",
            color = colors.muted,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<Track>,
    activeTrackKey: String?,
    likedCount: Int,
    onPlayTrack: (Track) -> Unit,
    onToggleLike: (Track) -> Unit
) {
    val colors = LocalAethraColors.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Your library.", color = colors.text, style = MaterialTheme.typography.headlineLarge, fontSize = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text("Saved music, ready when you are.", color = colors.muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        AethraCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(colors.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Liked music", color = colors.text, fontWeight = FontWeight.Bold)
                    Text(likedCount.toString() + " saved tracks", color = colors.muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (tracks.isEmpty()) {
            EmptyState("NOTHING SAVED YET", "Tap the heart beside a track to keep it in your library.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track, true, { onPlayTrack(track) }, { onToggleLike(track) }, activeTrackKey == track.stableKey)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    mode: ThemeMode,
    style: VisualStyle,
    onModeChange: (ThemeMode) -> Unit,
    onStyleChange: (VisualStyle) -> Unit
) {
    val colors = LocalAethraColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = colors.text, style = MaterialTheme.typography.headlineLarge, fontSize = 32.sp)
        Text("Make AETHRA feel right for you.", color = colors.muted, fontSize = 13.sp)
        SetupSectionTitle("APPEARANCE", "Choose your preferred canvas")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = "Light",
                selected = mode == ThemeMode.LIGHT,
                onClick = { onModeChange(ThemeMode.LIGHT) },
                icon = { Icon(Icons.Rounded.LightMode, contentDescription = null) }
            )
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = "Dark",
                selected = mode == ThemeMode.DARK,
                onClick = { onModeChange(ThemeMode.DARK) },
                icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) }
            )
        }
        SetupSectionTitle("ONLINE DISCOVERY", "YouTube search · no AETHRA account")
        AethraCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("YOUTUBE MUSIC DISCOVERY", color = colors.accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.3.sp)
                Text("AETHRA uses the official YouTube Data API to find music videos and show their titles and artwork. Tap a result to open it on YouTube. The search API does not provide playable audio streams for in-app playback.", color = colors.text, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        SetupSectionTitle("DESIGN LANGUAGE", "Change the complete visual character")
        VisualStyle.entries.forEach { option ->
            StyleChoice(option, style == option) { onStyleChange(option) }
        }
        AethraCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ABOUT AETHRA", color = colors.accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.3.sp)
                Text("AETHRA requires no account and inserts no AETHRA advertisements. Third-party music rights and availability remain governed by their providers.", color = colors.text, fontSize = 12.sp, lineHeight = 18.sp)
                Text("VERSION 1.6.1 · LOCAL PLAYBACK + YOUTUBE DISCOVERY", color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
    track: Track?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    queue: List<Track>,
    queueIndex: Int,
    onPlayQueueItem: (Int) -> Unit,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val colors = LocalAethraColors.current
    var queueOpen by remember { mutableStateOf(false) }
    val infinite = rememberInfiniteTransition(label = "cover-motion")
    val artScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cover-scale"
    )
    Column(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close player", tint = colors.text) }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { queueOpen = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Open queue", tint = colors.text) }
        }
        Spacer(Modifier.weight(0.7f))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(1f)
                .graphicsLayer {
                    scaleX = if (isPlaying) artScale else 1f
                    scaleY = if (isPlaying) artScale else 1f
                    shadowElevation = if (colors.glass) 30.dp.toPx() else 14.dp.toPx()
                    shape = RoundedCornerShape(if (colors.glass) 32.dp else 20.dp)
                    clip = true
                }
                .then(
                    if (colors.glass) Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.34f), Color(0xFF9FCBFF).copy(alpha = 0.22f),
                                    Color(0xFF5C83C8).copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f))
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(32.dp))
                    else Modifier
                )
                .padding(if (colors.glass) 9.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackArtwork(
                track = track,
                title = title,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(if (colors.glass) 25.dp else 20.dp))
            )
            if (colors.glass) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.24f), Color.Transparent, Color.Black.copy(alpha = 0.12f))
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx())
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.56f),
                        androidx.compose.ui.geometry.Offset(size.width * 0.12f, 1.dp.toPx()),
                        androidx.compose.ui.geometry.Offset(size.width * 0.72f, 1.dp.toPx()),
                        1.dp.toPx()
                    )
                }
            }
        }
        Spacer(Modifier.weight(0.8f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title.ifBlank { "Nothing playing" }, color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist.ifBlank { "Choose a track from your library" }, color = colors.muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(22.dp))
        val safeDuration = durationMs.coerceAtLeast(1L)
        Slider(
            value = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
            onValueChange = { onSeek((it * safeDuration).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.outline
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), color = colors.muted, fontSize = 11.sp)
            Text(formatTime(durationMs), color = colors.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = colors.text, modifier = Modifier.size(31.dp))
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(68.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(colors.accent)
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colors.background,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = colors.text, modifier = Modifier.size(31.dp))
            }
        }
        Spacer(Modifier.weight(0.7f))
    }

    if (queueOpen) {
        AlertDialog(
            onDismissRequest = { queueOpen = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Up next", color = colors.text, fontWeight = FontWeight.Bold)
                    Text("${queue.size} tracks in this queue", color = colors.muted, fontSize = 12.sp)
                }
            },
            text = {
                if (queue.isEmpty()) {
                    Text("Your queue is empty. Play a track to get started.", color = colors.muted)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(queue, key = { index, item -> "${item.stableKey}:$index" }) { index, item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (index == queueIndex) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable {
                                        onPlayQueueItem(index)
                                        queueOpen = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    if (index == queueIndex) "NOW" else (index + 1).toString().padStart(2, '0'),
                                    color = if (index == queueIndex) colors.accent else colors.muted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.title, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.artist, color = colors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(formatTime(item.duration), color = colors.muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { queueOpen = false }) { Text("DONE", color = colors.accent, fontWeight = FontWeight.Bold) }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}

@Composable
private fun MiniPlayer(
    track: Track?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalAethraColors.current
    AethraCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClick = onOpen)
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 9.dp, end = 4.dp, top = 8.dp, bottom = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(track, title, Modifier.size(48.dp))
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isPlaying) PlayingBars(Modifier.size(width = 13.dp, height = 12.dp), tint = colors.accent)
                        Text(artist, color = colors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onPlayPause) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play or pause", tint = colors.accent)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", tint = colors.text)
                }
            }
            Spacer(Modifier.height(7.dp))
            val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            Box(
                Modifier.fillMaxWidth().height(if (colors.glass) 2.dp else 2.dp)
                    .clip(CircleShape)
                    .background(colors.muted.copy(alpha = 0.22f))
            ) {
                Box(
                    Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(colors.accent, if (colors.glass) Color(0xFF9FE8F0) else colors.accentSecondary)))
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    isPlaying: Boolean = false
) {
    val colors = LocalAethraColors.current
    val cardColor by animateColorAsState(
        targetValue = if (isPlaying) colors.accent.copy(alpha = if (colors.glass) 0.18f else 0.13f) else colors.surface,
        animationSpec = tween(260), label = "track-card-active"
    )
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(if (colors.sharp) 2.dp else if (colors.glass) 20.dp else 16.dp))
            .background(cardColor)
            .then(if (colors.sharp) Modifier.border(1.5.dp, colors.text, RoundedCornerShape(2.dp)) else Modifier)
            .clickable(onClick = onPlay)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(track, track.title, Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (track.source == "YOUTUBE_VIDEO") track.artist + " · YOUTUBE" else track.artist,
                color = colors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (isPlaying) {
            PlayingBars(Modifier.padding(horizontal = 7.dp).size(width = 16.dp, height = 18.dp), tint = colors.accent)
        }
        Text(if (track.source == "YOUTUBE_VIDEO") "OPEN" else formatTime(track.duration), color = if (isPlaying) colors.accent else colors.muted, fontSize = 10.sp, fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal)
        IconButton(onClick = onLike) {
            Icon(
                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isLiked) "Remove from liked music" else "Add to liked music",
                tint = if (isLiked) colors.accentSecondary else colors.muted
            )
        }
    }
}

@Composable
private fun PlayingBars(modifier: Modifier = Modifier, tint: Color) {
    val transition = rememberInfiniteTransition(label = "aethra-playing-bars")
    val first by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "playing-bar-one"
    )
    val second by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(560), RepeatMode.Reverse),
        label = "playing-bar-two"
    )
    val third by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
        label = "playing-bar-three"
    )
    Canvas(modifier) {
        val barWidth = size.width / 5f
        listOf(first, second, third).forEachIndexed { index, level ->
            val barHeight = size.height * level
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(index * barWidth * 2f, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun TrackArtwork(track: Track?, title: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = track?.let { artworkCache.get(it.stableKey) }, key1 = track?.stableKey) {
        if (track != null && value == null) {
            value = withContext(Dispatchers.IO) {
                artworkCache.get(track.stableKey) ?: run {
                    val remote = track.artworkUrl?.let(::downloadArtwork)
                    (remote ?: if (track.source == "LOCAL") extractArtwork(context, track) else null)
                        ?.also { artworkCache.put(track.stableKey, it) }
                }
            }
        }
    }
    val colors = LocalAethraColors.current
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = title + " artwork",
            modifier = modifier.clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier.clip(RoundedCornerShape(14.dp)).background(
                Brush.linearGradient(listOf(colors.accent.copy(alpha = 0.88f), colors.accentSecondary.copy(alpha = 0.84f), colors.raised))
            ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.15f), size.minDimension * 0.42f, androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f))
                drawRect(Color.Black.copy(alpha = 0.08f), androidx.compose.ui.geometry.Offset(0f, size.height * 0.68f), androidx.compose.ui.geometry.Size(size.width, size.height * 0.32f))
            }
            Text(
                title.trim().firstOrNull()?.uppercase() ?: "A",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun downloadArtwork(url: String): Bitmap? {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 15_000
        instanceFollowRedirects = true
    }
    return try {
        if (connection.responseCode in 200..299) {
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } else null
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun extractArtwork(context: Context, track: Track): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, track.uri)
        val bytes = retriever.embeddedPicture ?: return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
private fun HeroPanel(count: Int, onShuffle: () -> Unit) {
    val colors = LocalAethraColors.current
    val shape = RoundedCornerShape(if (colors.sharp) 3.dp else if (colors.glass) 30.dp else 20.dp)
    var heroSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    Box(
        Modifier.fillMaxWidth()
            .height(198.dp)
            .onSizeChanged { heroSize = it }
            .then(
                if (colors.glass && heroSize.width > 0 && heroSize.height > 0) {
                    Modifier.liquidGlass(
                        lensCenter = androidx.compose.ui.geometry.Offset(heroSize.width * 0.82f, heroSize.height * 0.5f),
                        lensSize = androidx.compose.ui.geometry.Size(heroSize.width * 0.22f, heroSize.height * 0.58f),
                        cornerRadius = heroSize.height * 0.29f,
                        refraction = 0.16f,
                        curve = 0.24f,
                        dispersion = 0.008f,
                        saturation = 1.14f,
                        contrast = 1.10f,
                        edge = 0.62f
                    )
                } else Modifier
            )
            .clip(shape)
            .then(
                if (colors.sharp) Modifier.background(colors.raised)
                else Modifier.background(Brush.linearGradient(listOf(colors.accent, colors.accentSecondary, colors.raised)))
            )
            .then(if (colors.sharp) Modifier.border(2.dp, colors.text, shape) else Modifier)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (colors.sharp) {
                drawRect(colors.accent, topLeft = androidx.compose.ui.geometry.Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height))
                drawRect(colors.accentSecondary, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.48f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.66f))
                drawRect(colors.text, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.66f), size = androidx.compose.ui.geometry.Size(size.width * 0.28f, size.height * 0.34f))
                var x = 0f
                while (x < size.width) {
                    drawLine(colors.text.copy(alpha = 0.16f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.dp.toPx())
                    x += 28.dp.toPx()
                }
            } else {
                drawCircle(Color.White.copy(alpha = 0.16f), size.minDimension * 0.48f, androidx.compose.ui.geometry.Offset(size.width * 0.99f, size.height * 0.08f))
                drawCircle(Color.Black.copy(alpha = 0.08f), size.minDimension * 0.2f, androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.9f))
                if (!colors.glass) {
                    drawRect(Color.White.copy(alpha = 0.18f), androidx.compose.ui.geometry.Offset(size.width * 0.74f, size.height * 0.52f), androidx.compose.ui.geometry.Size(size.width * 0.17f, size.height * 0.13f))
                }
            }
        }
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("AETHRA  /  YOUR MUSIC", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Column {
                Text(
                    if (colors.glass) "Sound in motion." else if (colors.sharp) "MAKE\nROOM FOR\nMUSIC." else "Made for\nlistening.",
                    color = if (colors.sharp) Color(0xFF171717) else Color.White,
                    fontSize = 31.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp
                )
                Text(if (colors.sharp) "A NEW SOUND IS OUT THERE" else count.toString() + " TRACKS IN YOUR LIBRARY", color = if (colors.sharp) Color(0xFF171717) else Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onShuffle,
                    shape = RoundedCornerShape(if (colors.sharp) 2.dp else if (colors.glass) 24.dp else 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (colors.sharp) Color(0xFF171717) else Color.White, contentColor = if (colors.sharp) Color(0xFFFFBD2E) else Color(0xFF141619))
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("SHUFFLE", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(if (colors.sharp) "INDEPENDENT SOUND / ZERO AETHRA ADS" else "NO AETHRA ADS", color = if (colors.sharp) Color(0xFF171717) else Color.White.copy(alpha = 0.82f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun AethraCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AethraSurfaceCard(
        modifier = modifier,
        colors = LocalAethraColors.current,
        content = content
    )
}

@Composable
private fun SectionHeading(label: String, meta: String) {
    val colors = LocalAethraColors.current
    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
        Text(meta, color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    val colors = LocalAethraColors.current
    AethraCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.accent, modifier = Modifier.size(25.dp))
            Text(title, color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text(message, color = colors.text, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

private fun readTracks(context: Context): List<Track> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val columns = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DISPLAY_NAME
    )
    val result = mutableListOf<Track>()
    context.contentResolver.query(
        collection,
        columns,
        MediaStore.Audio.Media.IS_MUSIC + " != 0",
        null,
        MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val rawTitle = cursor.getString(titleColumn).orEmpty()
            val displayName = if (displayNameColumn >= 0) cursor.getString(displayNameColumn).orEmpty() else ""
            val fallbackTitle = displayName.substringBeforeLast('.', displayName).replace(Regex("[_-]+"), " ").trim()
            val looksLikeIdentifier = rawTitle.matches(Regex("[0-9a-fA-F-]{20,}")) || rawTitle.matches(Regex("[A-Z0-9]{18,}"))
            val cleanTitle = if (rawTitle.isBlank() || looksLikeIdentifier) fallbackTitle.ifBlank { "Unknown track" } else rawTitle
            result += Track(
                id = id,
                title = cleanTitle,
                artist = cursor.getString(artistColumn)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                album = cursor.getString(albumColumn) ?: "Unknown album",
                duration = cursor.getLong(durationColumn),
                uri = ContentUris.withAppendedId(collection, id)
            )
        }
    }
    return result.distinctBy { it.title.trim().lowercase() + "|" + it.artist.trim().lowercase() + "|" + (it.duration / 1000L) }
}

private fun Track.asMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(stableKey)
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    )
    .build()

private fun readVisualStyle(value: String?): VisualStyle = runCatching {
    VisualStyle.valueOf(value ?: VisualStyle.NEO_CONSTRUCTIVIST_POP.name)
}.getOrDefault(VisualStyle.NEO_CONSTRUCTIVIST_POP)

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
