package com.aethra.launcher.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DesktopApp(val title: String, val packageName: String, val activityName: String, val icon: Drawable)

private val FG = Color(0xFFF8FBFF)
private val AMBER = Color(0xFFFFC995)
private val CORAL = Color(0xFFFF886B)

@Composable
fun GoldenGateLauncher() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("golden_gate_desktop", Context.MODE_PRIVATE) }
    var apps by remember { mutableStateOf(emptyList<DesktopApp>()) }
    var pinned by remember { mutableStateOf(prefs.getStringSet("pinned_apps", emptySet())?.toSet() ?: emptySet()) }
    var query by remember { mutableStateOf("") }
    var drawer by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var labels by remember { mutableStateOf(prefs.getBoolean("show_labels", true)) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact_dock", false)) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var battery by remember { mutableIntStateOf(readBattery(context)) }
    var roleRefresh by remember { mutableIntStateOf(0) }
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    val roleManager = remember { if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { roleRefresh++ }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .mapNotNull { info ->
                    val a = info.activityInfo ?: return@mapNotNull null
                    if (a.packageName == context.packageName) return@mapNotNull null
                    DesktopApp(info.loadLabel(context.packageManager)?.toString()?.trim()?.ifEmpty { a.packageName } ?: a.packageName,
                        a.packageName, a.name, info.loadIcon(context.packageManager))
                }.distinctBy { it.packageName }.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }
    LaunchedEffect(Unit) {
        while (true) { clock = System.currentTimeMillis(); battery = readBattery(context); delay(30_000L) }
    }
    BackHandler(drawer || settings) {
        if (settings) settings = false else { drawer = false; query = "" }
    }

    val favourites = pinned.mapNotNull { key -> apps.firstOrNull { it.packageName == key } }
    val suggested = apps.filter { app ->
        listOf("chrome", "camera", "settings", "messages", "phone", "photos", "gallery", "youtube", "play")
            .any { app.packageName.contains(it, true) }
    }.take(8)
    val desktopApps = (favourites + if (suggested.isNotEmpty()) suggested else apps.take(12)).distinctBy { it.packageName }.take(12)
    val filteredApps = if (query.isBlank()) apps else apps.filter {
        it.title.contains(query.trim(), true) || it.packageName.contains(query.trim(), true)
    }
    val isDefault = if (Build.VERSION.SDK_INT >= 29) {
        roleRefresh
        roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
    } else false

    Box(Modifier.fillMaxSize().background(Color(0xFF061727))) {
        GoldenGateWallpaper(Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = (-120).dp).size(320.dp).blur(90.dp)
            .background(AMBER.copy(alpha = .22f), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-115).dp, y = 110.dp).size(300.dp).blur(90.dp)
            .background(Color(0xFF76D6E4).copy(alpha = .15f), CircleShape))

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 9.dp)) {
            MenuBar(clock, battery) { settings = true }
            Spacer(Modifier.height(if (wide) 23.dp else 17.dp))

            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(Modifier.width(205.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        ClockCard(clock)
                        GlassCard(Modifier.fillMaxWidth(), padding = 15.dp) {
                            Text("YOUR SPACE", color = FG.copy(alpha = .6f), fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("A little more\nroom to think.", color = FG, fontSize = 22.sp,
                                lineHeight = 26.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(9.dp))
                            Text("AETHRA  /  GOLDEN GATE", color = FG.copy(alpha = .58f), fontSize = 8.sp, letterSpacing = 1.1.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Hold an app to pin or unpin it.", color = FG.copy(alpha = .62f), fontSize = 10.sp)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        SearchBar(query, { query = it; drawer = true }, { drawer = true })
                        Spacer(Modifier.height(17.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Favourites", color = FG, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(desktopApps.size.toString() + " SHORTCUTS", color = FG.copy(alpha = .6f), fontSize = 9.sp, letterSpacing = 1.1.sp)
                        }
                        Spacer(Modifier.height(9.dp))
                        LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(desktopApps, key = { it.packageName }) { app ->
                                AppTile(app, pinned.contains(app.packageName), labels,
                                    { launchApp(context, app) }, { pinApp(app, pinned, prefs) { pinned = it } })
                            }
                        }
                    }
                }
            } else {
                Text("Golden Gate", color = FG, fontSize = 33.sp, lineHeight = 38.sp,
                    fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp)
                Text("A quieter place to begin.", color = FG.copy(alpha = .76f), fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                SearchBar(query, { query = it; drawer = true }, { drawer = true })
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Favourites", color = FG, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("HOLD TO PIN", color = FG.copy(alpha = .6f), fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(74.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(desktopApps, key = { it.packageName }) { app ->
                        AppTile(app, pinned.contains(app.packageName), labels,
                            { launchApp(context, app) }, { pinApp(app, pinned, prefs) { pinned = it } })
                    }
                }
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Dock((favourites + suggested).distinctBy { it.packageName }.take(if (wide) 6 else 4), compact,
                    { launchApp(context, it) }, { drawer = true }, { openFiles(context) }, { settings = true })
            }
        }

        AnimatedVisibility(drawer, Modifier.fillMaxSize()) {
            AppDrawer(filteredApps, query, { query = it }, pinned,
                { pinApp(it, pinned, prefs) { pinned = it } },
                { launchApp(context, it) }, { drawer = false; query = "" })
        }
        AnimatedVisibility(settings, Modifier.fillMaxSize()) {
            SettingsPanel(labels, { labels = it; prefs.edit().putBoolean("show_labels", it).apply() },
                compact, { compact = it; prefs.edit().putBoolean("compact_dock", it).apply() },
                isDefault,
                {
                    if (Build.VERSION.SDK_INT >= 29 && roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true)
                        roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    else runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                { runCatching { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                { settings = false })
        }
    }
}

@Composable
private fun GoldenGateWallpaper(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(0f to Color(0xFF061727), .34f to Color(0xFF16465E),
            .63f to Color(0xFF738C92), 1f to Color(0xFFB79C86), startY = 0f, endY = h))
        val sun = androidx.compose.ui.geometry.Offset(w * .78f, h * .27f)
        drawCircle(Brush.radialGradient(listOf(AMBER.copy(alpha = .42f), AMBER.copy(alpha = .09f), Color.Transparent), sun, w * .28f), w * .28f, sun)
        drawCircle(AMBER.copy(alpha = .72f), w * .043f, sun)
        val land = Path().apply {
            moveTo(0f, h * .55f); cubicTo(w * .12f, h * .46f, w * .20f, h * .49f, w * .31f, h * .53f)
            cubicTo(w * .46f, h * .47f, w * .55f, h * .50f, w * .67f, h * .55f)
            lineTo(w, h * .60f); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(land, Color(0xFF0D3043).copy(alpha = .55f))
        drawRect(Brush.verticalGradient(listOf(Color(0xFF5A8791).copy(alpha = .24f), Color(0xFF061A2A).copy(alpha = .94f)), h * .59f, h),
            androidx.compose.ui.geometry.Offset(0f, h * .59f), androidx.compose.ui.geometry.Size(w, h * .41f))
        val left = w * .30f; val right = w * .72f; val top = h * .34f; val deck = h * .70f
        val bridge = CORAL.copy(alpha = .72f)
        val cable = Path().apply {
            moveTo(w * .02f, h * .65f); quadraticTo(left, h * .38f, right, h * .65f)
            quadraticTo(w * .86f, h * .70f, w * .98f, h * .69f)
        }
        drawPath(cable, bridge, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        listOf(left, right).forEach { x ->
            drawLine(bridge, androidx.compose.ui.geometry.Offset(x, top), androidx.compose.ui.geometry.Offset(x, deck), 5.dp.toPx())
            drawLine(bridge, androidx.compose.ui.geometry.Offset(x - w * .025f, top), androidx.compose.ui.geometry.Offset(x + w * .025f, top), 3.dp.toPx())
            drawLine(bridge, androidx.compose.ui.geometry.Offset(x - w * .023f, deck), androidx.compose.ui.geometry.Offset(x + w * .023f, deck), 3.dp.toPx())
        }
        for (i in 1..18) {
            val x = left + (right - left) * i / 19f
            val t = ((x - left) / (right - left)) * 2f - 1f
            val y = h * .65f - h * .27f * (1f - t * t)
            drawLine(bridge.copy(alpha = .42f), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, deck), 1.dp.toPx())
        }
        drawLine(bridge, androidx.compose.ui.geometry.Offset(w * .02f, deck), androidx.compose.ui.geometry.Offset(w * .98f, deck), 2.4.dp.toPx())
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .62f to Color.Transparent, 1f to Color(0xFF041323).copy(alpha = .42f)))
    }
}

@Composable
private fun MenuBar(now: Long, battery: Int, onSettings: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), corner = 21.dp, padding = 9.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(27.dp).clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFAC82), Color(0xFFDC665B), Color(0xFF708D9E)))),
                contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
            Spacer(Modifier.width(9.dp))
            Text("AETHRA", color = FG, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
            Spacer(Modifier.width(16.dp))
            Text("Desktop", color = FG.copy(alpha = .86f), fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.BatteryFull, null, tint = FG.copy(alpha = .9f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp)); Text("$battery%", color = FG.copy(alpha = .9f), fontSize = 9.sp)
            Spacer(Modifier.width(11.dp))
            Text(SimpleDateFormat("EEE  d MMM  •  h:mm a", Locale.getDefault()).format(Date(now)),
                color = FG, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(7.dp)); GlassIcon(Icons.Rounded.Settings, "Settings", onSettings, 29.dp)
        }
    }
}

@Composable
private fun ClockCard(now: Long) {
    GlassCard(Modifier.fillMaxWidth(), padding = 15.dp) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = AMBER, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(5.dp))
        Text(SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)), color = FG.copy(alpha = .72f), fontSize = 10.sp)
        Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(now)), color = FG, fontSize = 43.sp,
            lineHeight = 47.sp, fontWeight = FontWeight.Light)
        Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(now)), color = FG.copy(alpha = .76f), fontSize = 10.sp)
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(47.dp).clip(CircleShape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .10f))))
        .border(1.dp, Color.White.copy(alpha = .25f), CircleShape).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = FG.copy(alpha = .88f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(query, onChange, Modifier.weight(1f), singleLine = true,
            textStyle = TextStyle(color = FG, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search apps and open anything", color = FG.copy(alpha = .67f), fontSize = 13.sp)
                inner()
            })
        Text("OPEN", color = FG.copy(alpha = .68f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = onOpen))
    }
}

@Composable
private fun AppDrawer(
    apps: List<DesktopApp>, query: String, onQuery: (String) -> Unit,
    pinned: Set<String>, onPin: (DesktopApp) -> Unit,
    onLaunch: (DesktopApp) -> Unit, onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xC9081420)).padding(13.dp), contentAlignment = Alignment.Center) {
        GlassCard(Modifier.fillMaxWidth().fillMaxHeight(.95f), corner = 29.dp, padding = 16.dp) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Launchpad", color = FG, fontSize = 23.sp, fontWeight = FontWeight.Light)
                        Text(apps.size.toString() + " APPLICATIONS", color = FG.copy(alpha = .6f), fontSize = 9.sp, letterSpacing = 1.4.sp)
                    }
                    Spacer(Modifier.weight(1f)); GlassIcon(Icons.Rounded.Close, "Close", onClose, 37.dp)
                }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha = .13f))
                    .border(1.dp, Color.White.copy(alpha = .18f), CircleShape).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = FG.copy(alpha = .8f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(query, onQuery, Modifier.weight(1f), singleLine = true,
                        textStyle = TextStyle(color = FG, fontSize = 14.sp),
                        decorationBox = { inner -> if (query.isEmpty()) Text("Find an app", color = FG.copy(alpha = .6f), fontSize = 14.sp); inner() })
                    if (query.isNotEmpty()) GlassIcon(Icons.Rounded.Close, "Clear", { onQuery("") }, 27.dp)
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(84.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        AppTile(app, pinned.contains(app.packageName), true, { onLaunch(app) }, { onPin(app) })
                    }
                }
                Text("HOLD AN APP TO PIN OR UNPIN IT", color = FG.copy(alpha = .54f), fontSize = 8.sp,
                    letterSpacing = .9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    labels: Boolean, onLabels: (Boolean) -> Unit, compact: Boolean, onCompact: (Boolean) -> Unit,
    isDefault: Boolean, onSetDefault: () -> Unit, onWallpaper: () -> Unit, onAppInfo: () -> Unit, onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xC9081420)).padding(14.dp), contentAlignment = Alignment.Center) {
        GlassCard(Modifier.fillMaxWidth().widthIn(max = 520.dp), corner = 28.dp, padding = 19.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Desktop settings", color = FG, fontSize = 22.sp, fontWeight = FontWeight.Light)
                        Text("Make this space feel like yours.", color = FG.copy(alpha = .64f), fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f)); GlassIcon(Icons.Rounded.Close, "Close", onClose)
                }
                PreferenceRow("Show app labels", "Names beneath desktop icons", labels, onLabels)
                PreferenceRow("Compact Dock", "A smaller floating Dock", compact, onCompact)
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .15f)))
                SettingAction("Default home app", if (isDefault) "AETHRA is your Home app" else "Choose AETHRA when you press Home", "SET", onSetDefault)
                SettingAction("System wallpaper", "Open Android wallpaper settings", "OPEN", onWallpaper, Icons.Rounded.Wallpaper)
                SettingAction("App information", "Permissions and app details", "OPEN", onAppInfo, Icons.Rounded.Home)
            }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = FG, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FG.copy(alpha = .62f), fontSize = 10.sp)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun SettingAction(
    title: String, subtitle: String, action: String, onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Home
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = FG.copy(alpha = .85f), modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = FG, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FG.copy(alpha = .62f), fontSize = 10.sp)
        }
        GlassPill(action, onClick)
    }
}

@Composable
private fun Dock(
    apps: List<DesktopApp>, compact: Boolean, onLaunch: (DesktopApp) -> Unit,
    onDrawer: () -> Unit, onFiles: () -> Unit, onSettings: () -> Unit
) {
    GlassCard(corner = 25.dp, padding = if (compact) 8.dp else 10.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            apps.forEach { DockApp(it, compact) { onLaunch(it) } }
            if (apps.isNotEmpty()) Spacer(Modifier.width(1.dp).height(35.dp).background(Color.White.copy(alpha = .3f)))
            DockAction(Icons.Rounded.Apps, "Launchpad", compact, onDrawer)
            DockAction(Icons.Rounded.FolderOpen, "Files", compact, onFiles)
            DockAction(Icons.Rounded.Settings, "Settings", compact, onSettings)
        }
    }
}

@Composable
private fun DockApp(app: DesktopApp, compact: Boolean, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap(112, 112).asImageBitmap() }
    var selected by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = .58f, stiffness = 420f), label = "dock-scale")
    Image(bitmap, app.title, Modifier.size(if (compact) 34.dp else 42.dp).scale(scale)
        .clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { selected = true; onClick() }, onLongClick = onClick))
}

@Composable
private fun DockAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, compact: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(if (compact) 35.dp else 42.dp).clip(RoundedCornerShape(12.dp))
        .background(Color.White.copy(alpha = .13f)).combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = FG, modifier = Modifier.size(if (compact) 19.dp else 22.dp))
    }
}

@Composable
private fun AppTile(app: DesktopApp, pinned: Boolean, labels: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap(128, 128).asImageBitmap() }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box {
            Image(bitmap, app.title, Modifier.size(47.dp).clip(RoundedCornerShape(13.dp)))
            if (pinned) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(15.dp)
                .clip(CircleShape).background(Color(0xFF18384B)).border(1.dp, Color.White.copy(alpha = .4f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFFB7A1), modifier = Modifier.size(9.dp))
            }
        }
        if (labels) Text(app.title, color = FG, fontSize = 9.sp, minLines = 1, maxLines = 2,
            lineHeight = 11.sp, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, corner: Dp = 22.dp, padding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(corner)
    Column(modifier.shadow(17.dp, shape, clip = false).clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .115f), Color.White.copy(alpha = .075f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .48f), Color.White.copy(alpha = .12f), Color.White.copy(alpha = .23f))), shape)
        .padding(padding), content = content)
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, size: Dp = 33.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = .13f))
        .border(1.dp, Color.White.copy(alpha = .18f), CircleShape)
        .combinedClickable(onClick = onClick, onLongClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = FG, modifier = Modifier.size(size * .48f))
    }
}

@Composable
private fun GlassPill(label: String, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = .17f))
        .border(1.dp, Color.White.copy(alpha = .25f), CircleShape)
        .combinedClickable(onClick = onClick, onLongClick = onClick)
        .padding(horizontal = 13.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = FG, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

private fun readBattery(context: Context): Int {
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 0
}

private fun pinApp(app: DesktopApp, current: Set<String>, prefs: android.content.SharedPreferences, update: (Set<String>) -> Unit) {
    val next = if (current.contains(app.packageName)) current - app.packageName else current + app.packageName
    update(next)
    prefs.edit().putStringSet("pinned_apps", next).apply()
}

private fun launchApp(context: Context, app: DesktopApp) {
    runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(app.packageName, app.activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openFiles(context: Context) {
    runCatching { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
