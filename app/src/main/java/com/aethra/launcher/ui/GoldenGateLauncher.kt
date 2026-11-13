package com.aethra.launcher.ui

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LauncherApp(val title: String, val packageName: String, val activityName: String, val icon: Drawable)

@Composable
fun GoldenGateLauncher() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var query by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { info: ResolveInfo ->
                    val activity = info.activityInfo ?: return@mapNotNull null
                    LauncherApp(
                        title = info.loadLabel(context.packageManager)?.toString() ?: activity.packageName,
                        packageName = activity.packageName,
                        activityName = activity.name,
                        icon = info.loadIcon(context.packageManager)
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    val filtered = apps.filter { it.title.contains(query, true) || it.packageName.contains(query, true) }
    val featured = apps.filter { app ->
        val p = app.packageName.lowercase()
        p.contains("chrome") || p.contains("camera") || p.contains("settings") ||
            p.contains("messages") || p.contains("phone") || p.contains("gallery") ||
            p.contains("photos") || p.contains("youtube") || p.contains("play")
    }.take(6)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF071A2B), Color(0xFF16445E), Color(0xFF3D7180), Color(0xFFB0A18A))
            )
        ))
        // Golden-hour atmospheric light over a deep coastal-blue desktop.
        Box(Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = (-90).dp).size(340.dp).blur(75.dp)
            .background(Color(0xFFFFC98B).copy(alpha = 0.34f), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-80).dp, y = 110.dp).size(300.dp).blur(80.dp)
            .background(Color(0xFF63B6C5).copy(alpha = 0.24f), CircleShape))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = if (wide) 42.dp else 22.dp, vertical = 12.dp)
        ) {
            // macOS-inspired menu bar
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Apps, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text("AETHRA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.width(18.dp))
                Text("Desktop", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.BatteryFull, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(SimpleDateFormat("EEE, d MMM  •  h:mm a", Locale.getDefault()).format(Date()),
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(if (wide) 38.dp else 24.dp))
            Text("Golden Gate", color = Color.White, fontSize = if (wide) 42.sp else 34.sp,
                fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp)
            Text("A quieter place to begin.", color = Color.White.copy(alpha = 0.76f), fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            // Spotlight-style app search
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { showAllApps = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it; showAllApps = true },
                    singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search apps", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                        inner()
                    }
                )
                Text("⌘  Space", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
            }

            Spacer(Modifier.height(22.dp))
            if (!showAllApps) {
                Text("Favourites", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    featured.take(if (wide) 6 else 4).forEach { app ->
                        AppTile(app, Modifier.weight(1f)) { launchApp(context, app) }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your apps", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("See all  →", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp,
                        modifier = Modifier.clickable { showAllApps = true })
                }
                Spacer(Modifier.height(10.dp))
                val previewApps = apps.filterNot { app -> featured.any { it.packageName == app.packageName } }.take(if (wide) 12 else 8)
                LazyVerticalGrid(columns = GridCells.Adaptive(if (wide) 86.dp else 72.dp),
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(previewApps, key = { it.packageName }) { app -> AppTile(app) { launchApp(context, app) } }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Applications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("CLOSE", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp,
                        modifier = Modifier.clickable { showAllApps = false; query = "" })
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(if (wide) 96.dp else 78.dp),
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.packageName }) { app -> AppTile(app) { launchApp(context, app) } }
                }
            }

            // Floating translucent dock
            Row(
                Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.27f), Color.White.copy(alpha = 0.13f))))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dockApps = (featured + apps.filter { it.packageName.contains("settings") || it.packageName.contains("camera") })
                    .distinctBy { it.packageName }.take(5)
                dockApps.forEach { app ->
                    Image(bitmap = remember(app.icon) { app.icon.toBitmap(96, 96).asImageBitmap() },
                        contentDescription = app.title, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { launchApp(context, app) })
                }
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.3f)))
                Icon(Icons.Rounded.Folder, "Files", tint = Color.White, modifier = Modifier.size(31.dp)
                    .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root"), "vnd.android.document/root").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
                Icon(Icons.Rounded.Settings, "Settings", tint = Color.White, modifier = Modifier.size(30.dp)
                    .clickable { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
            }
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = remember(app.icon) { app.icon.toBitmap(112, 112).asImageBitmap() }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, animationSpec = spring(), label = "app-scale")
    Column(modifier.scale(scale).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
        .padding(horizontal = 5.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Image(bitmap = icon, contentDescription = app.title, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)))
        Text(app.title, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

private fun launchApp(context: android.content.Context, app: LauncherApp) {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(app.packageName, app.activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
