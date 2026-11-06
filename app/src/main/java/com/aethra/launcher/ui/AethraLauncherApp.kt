package com.aethra.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.aethra.launcher.data.LauncherApp
import com.aethra.launcher.data.LauncherRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AethraLauncherApp() {
    val context = LocalContext.current
    val repository = remember { LauncherRepository(context) }

    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var favorites by remember { mutableStateOf(repository.favoriteIds()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { repository.loadApps() }
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    BackHandler(enabled = drawerOpen || showAbout) {
        if (showAbout) showAbout = false else drawerOpen = false
    }

    val openDrawer: () -> Unit = {
        query = ""
        favoritesOnly = false
        drawerOpen = true
    }
    val toggleFavorite: (LauncherApp) -> Unit = { app ->
        favorites = repository.toggleFavorite(app.id, favorites)
    }
    val launchApp: (LauncherApp) -> Unit = { app ->
        repository.launch(app)
    }

    AethraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AethraPalette.Ink)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (drawerOpen) {
                AppDrawerScreen(
                    apps = apps,
                    favorites = favorites,
                    query = query,
                    favoritesOnly = favoritesOnly,
                    onQueryChange = { query = it },
                    onFavoritesOnlyChange = { favoritesOnly = it },
                    onBack = { drawerOpen = false },
                    onLaunch = launchApp,
                    onToggleFavorite = toggleFavorite
                )
            } else {
                HomeScreen(
                    apps = apps,
                    favorites = favorites,
                    now = now,
                    onOpenApps = openDrawer,
                    onAbout = { showAbout = true },
                    onLaunch = launchApp,
                    onToggleFavorite = toggleFavorite
                )
            }

            if (showAbout) {
                AboutDialog(onDismiss = { showAbout = false })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    apps: List<LauncherApp>,
    favorites: Set<String>,
    now: Long,
    onOpenApps: () -> Unit,
    onAbout: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit
) {
    val savedApps = apps.filter { it.id in favorites }
    val homeApps = (if (savedApps.isNotEmpty()) savedApps else apps).take(8)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onOpenApps) {
                var dragTotal = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        dragTotal += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (dragTotal < -76f) onOpenApps()
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f }
                )
            }
    ) {
        val wide = maxWidth >= 720.dp

        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.92f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    BrandHeader(onAbout)
                    ClockPanel(
                        now = now,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    SearchPrompt(onOpenApps)
                    Text(
                        text = "SWIPE UP OR OPEN THE APP FIELD",
                        color = AethraPalette.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1.08f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("YOUR SPACE", color = AethraPalette.Lime, fontSize = 11.sp, letterSpacing = 2.sp)
                            Text("At a glance.", color = AethraPalette.Paper, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (savedApps.isEmpty()) "SUGGESTED" else "SAVED",
                            color = AethraPalette.Muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.4.sp
                        )
                    }

                    if (homeApps.isEmpty()) {
                        EmptyAppsCard(Modifier.weight(1f))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 104.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(homeApps, key = { it.id }) { app ->
                                HomeAppTile(
                                    app = app,
                                    saved = app.id in favorites,
                                    onClick = { onLaunch(app) },
                                    onLongClick = { onToggleFavorite(app) }
                                )
                            }
                        }
                    }

                    BottomAction(onOpenApps, isWide = true)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandHeader(onAbout)
                ClockPanel(
                    now = now,
                    modifier = Modifier.fillMaxWidth().height(208.dp)
                )
                SearchPrompt(onOpenApps)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YOUR SPACE", color = AethraPalette.Lime, fontSize = 10.sp, letterSpacing = 1.8.sp)
                        Text("Quick access", color = AethraPalette.Paper, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (savedApps.isEmpty()) "SUGGESTED" else "SAVED",
                        color = AethraPalette.Muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.1.sp
                    )
                }

                if (homeApps.isEmpty()) {
                    EmptyAppsCard(Modifier.weight(1f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(homeApps, key = { it.id }) { app ->
                            HomeAppTile(
                                app = app,
                                saved = app.id in favorites,
                                onClick = { onLaunch(app) },
                                onLongClick = { onToggleFavorite(app) }
                            )
                        }
                    }
                }

                BottomAction(onOpenApps, isWide = false)
            }
        }
    }
}

@Composable
private fun BrandHeader(onAbout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark(Modifier.size(40.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("AETHRA", color = AethraPalette.Paper, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Text("BEYOND THE INTERFACE", color = AethraPalette.Muted, fontSize = 8.sp, letterSpacing = 1.25.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AethraPalette.Panel)
                .clickable(onClick = onAbout)
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Text("ABOUT", color = AethraPalette.Paper, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val shape = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.08f)
            lineTo(size.width * 0.92f, size.height * 0.86f)
            lineTo(size.width * 0.08f, size.height * 0.86f)
            close()
        }
        drawPath(shape, AethraPalette.Lime, style = Stroke(width = 3.2.dp.toPx()))
        drawLine(
            color = AethraPalette.Orange,
            start = Offset(size.width * 0.32f, size.height * 0.86f),
            end = Offset(size.width * 0.5f, size.height * 0.51f),
            strokeWidth = 3.2.dp.toPx()
        )
        drawLine(
            color = AethraPalette.Orange,
            start = Offset(size.width * 0.5f, size.height * 0.51f),
            end = Offset(size.width * 0.68f, size.height * 0.86f),
            strokeWidth = 3.2.dp.toPx()
        )
    }
}

@Composable
private fun ClockPanel(now: Long, modifier: Modifier = Modifier) {
    val calendar = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val time = remember(now) {
        String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
    val date = remember(now) {
        SimpleDateFormat("EEEE  ·  d MMMM", Locale.getDefault()).format(Date(now)).uppercase(Locale.getDefault())
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(AethraPalette.Paper)
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = AethraPalette.Lime,
                radius = size.minDimension * 0.43f,
                center = Offset(size.width * 0.98f, size.height * 0.08f)
            )
            drawCircle(
                color = AethraPalette.Orange,
                radius = size.minDimension * 0.105f,
                center = Offset(size.width * 0.88f, size.height * 0.80f)
            )
            drawRoundRect(
                color = AethraPalette.Ink.copy(alpha = 0.10f),
                topLeft = Offset(size.width * 0.77f, size.height * 0.55f),
                size = Size(size.width * 0.17f, size.height * 0.09f),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(AethraPalette.Orange))
                Spacer(Modifier.width(8.dp))
                Text("LOCAL TIME / FIELD 01", color = AethraPalette.Ink.copy(alpha = 0.66f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            }

            Column {
                Text(
                    text = time,
                    color = AethraPalette.Ink,
                    fontSize = 58.sp,
                    lineHeight = 62.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2.5).sp
                )
                Spacer(Modifier.height(7.dp))
                Text(date, color = AethraPalette.Ink.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Text("MAKE ROOM FOR WHAT MATTERS.", color = AethraPalette.Ink.copy(alpha = 0.72f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun SearchPrompt(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AethraPalette.Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌕", color = AethraPalette.Lime, fontSize = 25.sp, lineHeight = 25.sp)
        Spacer(Modifier.width(12.dp))
        Text("Find an app or open your app field", color = AethraPalette.Paper, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("SEARCH", color = AethraPalette.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppTile(
    app: LauncherApp,
    saved: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(57.dp)
                .shadow(5.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AethraPalette.PanelRaised),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(app, 39.dp)
            if (saved) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .size(7.dp).clip(CircleShape).background(AethraPalette.Lime)
                )
            }
        }
        Text(
            text = app.label,
            color = AethraPalette.Paper,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyAppsCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(AethraPalette.Panel).padding(22.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("THE FIELD IS QUIET.", color = AethraPalette.Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Text("Install an app and it will appear here.", color = AethraPalette.Paper, fontSize = 14.sp)
    }
}

@Composable
private fun BottomAction(onOpenApps: () -> Unit, isWide: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isWide) "AETHRA FORM  /  0.1.0" else "FORM 01  ·  0.1.0",
            color = AethraPalette.Muted,
            fontSize = 9.sp,
            letterSpacing = 1.1.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AethraPalette.Lime)
                .clickable(onClick = onOpenApps)
                .padding(horizontal = 18.dp, vertical = 13.dp)
        ) {
            Text("OPEN APP FIELD  ↗", color = AethraPalette.Ink, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerScreen(
    apps: List<LauncherApp>,
    favorites: Set<String>,
    query: String,
    favoritesOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit
) {
    val filtered = apps.filter { app ->
        (app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)) &&
            (!favoritesOnly || app.id in favorites)
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(AethraPalette.Panel)
                    .clickable(onClick = onBack).padding(horizontal = 15.dp, vertical = 11.dp)
            ) {
                Text("←  SPACE", color = AethraPalette.Paper, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("AETHRA  /  FORM 01", color = AethraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("THE APP FIELD", color = AethraPalette.Paper, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
            Text("EVERYTHING INSTALLED. NOTHING IN THE WAY.", color = AethraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search apps or package names", color = AethraPalette.Muted, fontSize = 13.sp) },
            shape = RoundedCornerShape(19.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AethraPalette.Lime,
                unfocusedBorderColor = AethraPalette.Line,
                focusedTextColor = AethraPalette.Paper,
                unfocusedTextColor = AethraPalette.Paper,
                cursorColor = AethraPalette.Lime,
                focusedContainerColor = AethraPalette.Panel,
                unfocusedContainerColor = AethraPalette.Panel
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterPill("ALL APPS", selected = !favoritesOnly) { onFavoritesOnlyChange(false) }
            Spacer(Modifier.width(8.dp))
            FilterPill("SAVED", selected = favoritesOnly) { onFavoritesOnlyChange(true) }
            Spacer(Modifier.weight(1f))
            Text(filtered.size.toString() + " ITEMS", color = AethraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (apps.isEmpty()) "NO APPS FOUND" else "NO MATCHES",
                    color = AethraPalette.Lime, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (apps.isEmpty()) "AETHRA could not find launchable apps on this profile." else "Try another name or switch back to all apps.",
                    color = AethraPalette.Muted, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { app ->
                    DrawerAppTile(
                        app = app,
                        saved = app.id in favorites,
                        onClick = { onLaunch(app) },
                        onLongClick = { onToggleFavorite(app) }
                    )
                }
            }
        }

        Text(
            "HOLD AN APP TO SAVE IT TO YOUR SPACE",
            color = AethraPalette.Muted,
            fontSize = 9.sp,
            letterSpacing = 1.35.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AethraPalette.Lime else AethraPalette.Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) AethraPalette.Ink else AethraPalette.Paper,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.15.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppTile(
    app: LauncherApp,
    saved: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(59.dp).clip(RoundedCornerShape(21.dp)).background(AethraPalette.PanelRaised),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(app, 42.dp)
        }
        Text(
            app.label,
            color = AethraPalette.Paper,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            if (saved) "SAVED" else " ",
            color = AethraPalette.Lime,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AethraPalette.Paper,
        titleContentColor = AethraPalette.Ink,
        textContentColor = AethraPalette.Ink,
        title = {
            Text("AETHRA", fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("BEYOND THE INTERFACE.", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text(
                    "AETHRA is an independent launcher for phones and tablets. This first build focuses on a clear home space, app search, app launching, and saved shortcuts.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Text("VERSION 0.1.0  ·  EARLY PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Your saved shortcuts are stored on this device. AETHRA does not include analytics or an account system in this version.", fontSize = 11.sp, lineHeight = 16.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AethraPalette.Ink,
                    contentColor = AethraPalette.Paper
                )
            ) {
                Text("BACK TO SPACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    )
}
