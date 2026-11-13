@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aethra.launcher.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val AWhite = Color(0xFFF7F8FC)
private val ASecondary = Color(0xFFB9C3D2)
private val ABlue = Color(0xFF83CFFF)
private val AGlass = Color(0xD91B2638)
private val AStroke = Color(0x40FFFFFF)

private enum class DesktopPhase { BOOT, LOCK, DESKTOP }
private data class DesktopApp(val id:String,val title:String,val icon:androidx.compose.ui.graphics.vector.ImageVector,val tint:Color)
private data class DeviceApp(val title:String,val packageName:String,val activity:String,val icon:Drawable)
private data class DesktopWindow(val id:String,val title:String,val x:Float,val y:Float,val workspace:Int,val minimized:Boolean=false,val maximized:Boolean=false)

private val desktopApps = listOf(
 DesktopApp("finder","Finder",Icons.Rounded.Folder,Color(0xFF68BDF4)),
 DesktopApp("browser","Safari",Icons.Rounded.Language,Color(0xFF5DCBE8)),
 DesktopApp("notes","Notes",Icons.Rounded.Notes,Color(0xFFFFD66B)),
 DesktopApp("calendar","Calendar",Icons.Rounded.CalendarMonth,Color(0xFFFF777C)),
 DesktopApp("photos","Photos",Icons.Rounded.PhotoLibrary,Color(0xFFCB8CF4)),
 DesktopApp("settings","System Settings",Icons.Rounded.Settings,Color(0xFFAAB8C8)),
 DesktopApp("mail","Mail",Icons.Rounded.Mail,Color(0xFF75BCEB)),
 DesktopApp("maps","Maps",Icons.Rounded.Map,Color(0xFF73C99C)),
 DesktopApp("clock","Clock",Icons.Rounded.AccessTime,Color(0xFF9EAAEA)),
 DesktopApp("contacts","Contacts",Icons.Rounded.Contacts,Color(0xFFFFA7B7)),
 DesktopApp("reminders","Reminders",Icons.Rounded.Checklist,Color(0xFFFFC56E)),
 DesktopApp("terminal","Terminal",Icons.Rounded.Code,Color(0xFF91D6B1)),
 DesktopApp("store","App Store",Icons.Rounded.Store,Color(0xFF75A9FF)),
 DesktopApp("android","Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF83D5A9))
)

@Composable
fun AethraFullDesktop() {
 val context= LocalContext.current
 val wide= LocalConfiguration.current.screenWidthDp >= 700
 val prefs= remember(context) { context.getSharedPreferences("aethra_full_desktop",Context.MODE_PRIVATE) }
 var phase by rememberSaveable { mutableStateOf(DesktopPhase.BOOT.name) }
 var bootProgress by remember { mutableFloatStateOf(0f) }
 var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
 var battery by remember { mutableIntStateOf(0) }
 var launchpad by remember { mutableStateOf(false) }
 var spotlight by remember { mutableStateOf(false) }
 var controlCenter by remember { mutableStateOf(false) }
 var notificationCenter by remember { mutableStateOf(false) }
 var missionControl by remember { mutableStateOf(false) }
 var search by remember { mutableStateOf("") }
 var workspace by rememberSaveable { mutableIntStateOf(1) }
 var windows by remember { mutableStateOf(listOf<DesktopWindow>()) }
 var notes by remember { mutableStateOf(prefs.getString("notes","Welcome to AETHRA.\n\nA new desktop, a new beginning.") ?: "") }
 var noteInput by remember { mutableStateOf("") }
 var reminderInput by remember { mutableStateOf("") }
 var reminders by remember { mutableStateOf(listOf<String>()) }
 var brightness by rememberSaveable { mutableFloatStateOf(.72f) }
 var wifiPreview by rememberSaveable { mutableStateOf(true) }
 var bluetoothPreview by rememberSaveable { mutableStateOf(false) }
 var focusPreview by rememberSaveable { mutableStateOf(false) }
 var installed by remember { mutableStateOf(emptyList<DeviceApp>()) }
 var frontWindow by remember { mutableStateOf<String?>(null) }

 LaunchedEffect(Unit) {
  for(i in 1..100) { bootProgress=i/100f; delay(27) }
  delay(280)
  phase=DesktopPhase.LOCK.name
 }
 LaunchedEffect(Unit) {
  while(true) {
   clock=System.currentTimeMillis()
   battery=(context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
    ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0,100) ?: 0
   delay(15000)
  }
 }
 LaunchedEffect(Unit) {
  installed=withContext(Dispatchers.IO) {
   @Suppress("DEPRECATION")
   context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
    .mapNotNull { info ->
     val a=info.activityInfo ?: return@mapNotNull null
     if(a.packageName==context.packageName) return@mapNotNull null
     DeviceApp(info.loadLabel(context.packageManager).toString(),a.packageName,a.name,info.loadIcon(context.packageManager))
    }.distinctBy { it.packageName }.sortedBy { it.title.lowercase(Locale.ROOT) }
  }
 }
 fun openApp(id:String) {
  val old=windows.firstOrNull { it.id==id }
  val next=if(old==null) windows+DesktopWindow(id,desktopApps.firstOrNull{it.id==id}?.title ?: id.replaceFirstChar{it.uppercase()},if(wide)180f else 18f,if(wide)90f else 70f,workspace)
   else { workspace=old.workspace; windows.map { if(it.id==id) it.copy(minimized=false) else it } }
  windows=next
  frontWindow=id
  launchpad=false;spotlight=false;missionControl=false;controlCenter=false;notificationCenter=false
 }
 fun closeWindow(id:String) {
  windows=windows.filterNot { it.id==id }
  frontWindow=windows.lastOrNull { !it.minimized }?.id
 }
 fun launchDeviceApp(app:DeviceApp) {
  runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.packageName,app.activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  launchpad=false;spotlight=false;missionControl=false
 }
 BackHandler(launchpad || spotlight || controlCenter || notificationCenter || missionControl || frontWindow!=null) {
  when {
   phase==DesktopPhase.LOCK.name -> phase=DesktopPhase.DESKTOP.name
   spotlight -> {spotlight=false;search=""}
   launchpad -> launchpad=false
   controlCenter -> controlCenter=false
   notificationCenter -> notificationCenter=false
   missionControl -> missionControl=false
   frontWindow!=null -> closeWindow(frontWindow!!)
  }
 }

 BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
  val maxW=maxWidth
  val maxH=maxHeight
  when(phase) {
   DesktopPhase.BOOT.name -> AethraBoot(bootProgress)
   DesktopPhase.LOCK.name -> AethraLockScreen(clock,battery) { phase=DesktopPhase.DESKTOP.name }
   else -> {
    AethraWallpaper(Modifier.fillMaxSize())
    Column(Modifier.fillMaxSize()) {
     AethraMenuBar(clock,battery,
      { launchpad=true },
      { spotlight=true },
      { controlCenter=true },
      { notificationCenter=true },
      { missionControl=true },
      { runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
     Box(Modifier.weight(1f).fillMaxWidth()
      .pointerInput(workspace) {
       var drag=0f
       detectHorizontalDragGestures(onHorizontalDrag={_,amount->drag+=amount},onDragEnd={
        if(drag < -90f) workspace=(workspace+1).coerceAtMost(4)
        if(drag > 90f) workspace=(workspace-1).coerceAtLeast(1)
        drag=0f
       })
      }) {
      // Desktop items are deliberately quiet: windows are the primary interaction surface.
      Column(Modifier.align(Alignment.TopStart).padding(start=if(wide)28.dp else 16.dp,top=20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
       DesktopShortcut("Macintosh HD",Icons.Rounded.Computer,Color(0xFFB8D5EB)){openApp("finder")}
       DesktopShortcut("AETHRA",Icons.Rounded.Folder,Color(0xFF79BCEB)){openApp("finder")}
      }
      windows.filter { !it.minimized && it.workspace==workspace }.forEach { win ->
       key(win.id) {
        AethraWindow(
         win=win,front=frontWindow==win.id,screenW=maxW,screenH=maxH,wide=wide,
         onFocus={frontWindow=win.id},
         onMove={dx,dy->windows=windows.map{if(it.id==win.id)it.copy(x=(it.x+dx).coerceIn(0f,(maxW.value-260f).coerceAtLeast(0f)),y=(it.y+dy).coerceIn(0f,(maxH.value-170f).coerceAtLeast(0f)))else it}},
         onMinimize={windows=windows.map{if(it.id==win.id)it.copy(minimized=true)else it}},
         onMaximize={windows=windows.map{if(it.id==win.id)it.copy(maximized=!it.maximized)else it}},
         onClose={closeWindow(win.id)}
        ) {
         when(win.id) {
          "finder" -> FinderContent(context)
          "browser" -> BrowserContent(context)
          "notes" -> NotesContent(notes,{notes=it;prefs.edit().putString("notes",it).apply()})
          "calendar" -> CalendarContent(context)
          "photos" -> PhotosContent(context)
          "settings" -> SettingsContent(context,wifiPreview,{wifiPreview=it},bluetoothPreview,{bluetoothPreview=it},focusPreview,{focusPreview=it},brightness,{brightness=it})
          "mail" -> NativeAppContent("Mail","Write an email using your preferred Android mail app.",Icons.Rounded.Mail){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "maps" -> NativeAppContent("Maps","Open a map with Android's map handler.",Icons.Rounded.Map){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "clock" -> NativeAppContent("Clock","Your tablet's alarms and timers.",Icons.Rounded.AccessTime){context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "contacts" -> NativeAppContent("Contacts","Your contacts remain managed by Android.",Icons.Rounded.Contacts){context.startActivity(Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "reminders" -> RemindersContent(reminderInput,{reminderInput=it},reminders,{reminders=it})
          "terminal" -> TerminalContent()
          "store" -> NativeAppContent("App Store","Browse apps installed through your Android app store.",Icons.Rounded.Store){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.android.chrome")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "android" -> AndroidAppsContent(installed,::launchDeviceApp)
         }
        }
       }
      }
      if(windows.none { !it.minimized && it.workspace==workspace }) {
       Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
        Text("Welcome to AETHRA",color=AWhite,fontSize=if(wide)34.sp else 27.sp,fontWeight=FontWeight.Light)
        Text("Your desktop. Your space.",color=ASecondary,fontSize=13.sp)
        Spacer(Modifier.height(13.dp))
        Text("Double tap an app in the Dock or open Launchpad.",color=ASecondary.copy(alpha=.8f),fontSize=10.sp)
       }
      }
      Text("Desktop  $workspace / 4",color=AWhite.copy(alpha=.75f),fontSize=9.sp,
       modifier=Modifier.align(Alignment.BottomEnd).padding(15.dp))
     }
     AethraDock(
      open={openApp(it)},
      launchpad={launchpad=true},
      minimized=windows.filter{it.minimized && it.workspace==workspace}.map{it.id},
      restore={id->windows=windows.map{if(it.id==id)it.copy(minimized=false)else it};frontWindow=id}
     )
    }
    if(launchpad) AethraLaunchpad(search,{search=it},desktopApps,installed,::openApp,::launchDeviceApp){launchpad=false;search=""}
    if(spotlight) AethraSpotlight(search,{search=it},desktopApps,installed,::openApp,::launchDeviceApp){spotlight=false;search=""}
    if(controlCenter) AethraControlCenter(wifiPreview,{wifiPreview=it},bluetoothPreview,{bluetoothPreview=it},focusPreview,{focusPreview=it},brightness,{brightness=it},{controlCenter=false})
    if(notificationCenter) AethraNotifications(clock){notificationCenter=false}
    if(missionControl) AethraMissionControl(windows.filter{!it.minimized},workspace,{workspace=it},::openApp){missionControl=false}
   }
  }
 }
}

@Composable
private fun AethraBoot(progress:Float) {
 val p by animateFloatAsState(progress, tween(250),label="boot")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF071322),Color(0xFF101E31),Color(0xFF070B13)))),contentAlignment=Alignment.Center) {
  Column(horizontalAlignment=Alignment.CenterHorizontally) {
   Box(Modifier.size(76.dp).clip(RoundedCornerShape(23.dp)).background(Brush.linearGradient(listOf(Color(0xFF9AD8F6),Color(0xFF3B6E9B)))),contentAlignment=Alignment.Center) {
    Text("A",color=Color.White,fontSize=48.sp,fontWeight=FontWeight.Light)
   }
   Spacer(Modifier.height(19.dp))
   Text("AETHRA",color=AWhite,fontSize=23.sp,fontWeight=FontWeight.Light,letterSpacing=6.sp)
   Spacer(Modifier.height(38.dp))
   Box(Modifier.width(150.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha=.15f))) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(AWhite.copy(alpha=.9f)))
   }
   Spacer(Modifier.height(11.dp))
   Text("Starting your desktop",color=ASecondary,fontSize=10.sp,letterSpacing=1.sp)
  }
 }
}

@Composable
private fun AethraLockScreen(clock:Long,battery:Int,onUnlock:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 val date=remember(clock,locale){SimpleDateFormat("EEEE, d MMMM",locale).format(Date(clock))}
 val time=remember(clock){SimpleDateFormat("h:mm",Locale.getDefault()).format(Date(clock))}
 Box(Modifier.fillMaxSize()) {
  AethraWallpaper(Modifier.fillMaxSize())
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x44030A18),Color(0x22030A18),Color(0x99030A18)))))
  Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
   Text(time,color=Color.White,fontSize=if(LocalConfiguration.current.screenWidthDp>=700)94.sp else 76.sp,fontWeight=FontWeight.ExtraLight,letterSpacing=(-4).sp)
   Text(date,color=Color.White.copy(alpha=.92f),fontSize=17.sp,fontWeight=FontWeight.Light)
   Spacer(Modifier.height(34.dp))
   Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha=.16f)).border(1.dp,Color.White.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center) {
    Icon(Icons.Rounded.Lock,null,tint=Color.White,modifier=Modifier.size(23.dp))
   }
   Spacer(Modifier.height(18.dp))
   Text("AETHRA",color=Color.White,fontSize=12.sp,letterSpacing=3.sp)
   Text("Tap to enter your desktop",color=Color.White.copy(alpha=.76f),fontSize=11.sp)
   Spacer(Modifier.height(16.dp))
   Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.18f)).border(1.dp,Color.White.copy(alpha=.35f),CircleShape)
    .clickable(onClick=onUnlock).padding(horizontal=25.dp,vertical=12.dp)) {
    Text("Unlock",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Medium)
   }
  }
  Row(Modifier.align(Alignment.BottomCenter).padding(26.dp),verticalAlignment=Alignment.CenterVertically) {
   Icon(Icons.Rounded.BatteryFull,null,tint=Color.White.copy(alpha=.8f),modifier=Modifier.size(15.dp))
   Text("  $battery%",color=Color.White.copy(alpha=.8f),fontSize=10.sp)
  }
 }
}

@Composable
private fun AethraWallpaper(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width;val h=size.height
  drawRect(Brush.verticalGradient(listOf(Color(0xFF102A48),Color(0xFF416F91),Color(0xFFE1A27D),Color(0xFF182B43))))
  drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD6A2).copy(alpha=.95f),Color(0xFFFFB47F).copy(alpha=.15f),Color.Transparent),center=Offset(w*.76f,h*.34f),radius=h*.24f),radius=h*.24f,center=Offset(w*.76f,h*.34f))
  val water=Path().apply{moveTo(0f,h*.68f);lineTo(w,h*.65f);lineTo(w,h);lineTo(0f,h);close()}
  drawPath(water,Brush.verticalGradient(listOf(Color(0xFF345C78),Color(0xFF101D31)),startY=h*.65f,endY=h))
  val bridge=Path().apply {
   moveTo(w*.04f,h*.68f);lineTo(w*.96f,h*.65f);moveTo(w*.22f,h*.67f);lineTo(w*.22f,h*.27f);moveTo(w*.78f,h*.65f);lineTo(w*.78f,h*.25f)
   moveTo(w*.22f,h*.28f);cubicTo(w*.35f,h*.38f,w*.65f,h*.38f,w*.78f,h*.26f)
  }
  drawPath(bridge,Color(0xFFE5A982).copy(alpha=.85f),style=androidx.compose.ui.graphics.drawscope.Stroke(width=(h*.004f).coerceAtLeast(2f),cap=StrokeCap.Round))
  for(i in 0..14) {
   val x=w*(.22f+i*.04f)
   drawLine(Color(0xFFFFD2AC).copy(alpha=.5f),Offset(x,h*.31f),Offset(x,h*.66f),strokeWidth=1.2f)
  }
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0x55040B16))),topLeft=Offset.Zero,size=size)
 }
}

@Composable
private fun AethraMenuBar(clock:Long,battery:Int,onLaunch:()->Unit,onSearch:()->Unit,onControl:()->Unit,onNotifications:()->Unit,onMission:()->Unit,onHomeSettings:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 val time=remember(clock,locale){SimpleDateFormat("EEE d MMM  h:mm",locale).format(Date(clock))}
 Row(Modifier.fillMaxWidth().height(34.dp).background(Color(0xB817263A)).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically) {
  Text("A",color=AWhite,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable(onClick=onLaunch))
  Text("  AETHRA",color=AWhite,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.clickable(onClick=onHomeSettings))
  Spacer(Modifier.width(18.dp))
  Text("File",color=AWhite.copy(alpha=.86f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onLaunch))
  Text("  Edit",color=AWhite.copy(alpha=.86f),fontSize=10.sp)
  Text("  View",color=AWhite.copy(alpha=.86f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onMission))
  Text("  Window",color=AWhite.copy(alpha=.86f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onMission))
  Spacer(Modifier.weight(1f))
  Icon(Icons.Rounded.Search,null,tint=AWhite,modifier=Modifier.size(17.dp).clickable(onClick=onSearch))
  Spacer(Modifier.width(11.dp))
  Icon(Icons.Rounded.Tune,null,tint=AWhite,modifier=Modifier.size(17.dp).clickable(onClick=onControl))
  Spacer(Modifier.width(11.dp))
  Icon(Icons.Rounded.Notifications,null,tint=AWhite,modifier=Modifier.size(16.dp).clickable(onClick=onNotifications))
  Spacer(Modifier.width(10.dp))
  Icon(Icons.Rounded.BatteryFull,null,tint=AWhite,modifier=Modifier.size(16.dp))
  Spacer(Modifier.width(4.dp))
  Text("$battery%",color=AWhite,fontSize=9.sp)
  Spacer(Modifier.width(9.dp))
  Text(time,color=AWhite,fontSize=9.sp,maxLines=1)
 }
}

@Composable
private fun DesktopShortcut(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit) {
 Column(Modifier.width(82.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Icon(icon,null,tint=tint,modifier=Modifier.size(36.dp))
  Text(title,color=AWhite,fontSize=9.sp,textAlign=TextAlign.Center,maxLines=2)
 }
}

@Composable
private fun AethraDock(open:(String)->Unit,launchpad:()->Unit,minimized:List<String>,restore:(String)->Unit) {
 Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
  Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xD91B2A3D)).border(1.dp,AStroke,RoundedCornerShape(22.dp)).padding(horizontal=7.dp,vertical=7.dp),
   horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
   DockIcon("Launchpad",Icons.Rounded.Apps,Color(0xFFB7A5F7),launchpad)
   desktopApps.filter{it.id in listOf("finder","browser","notes","photos","calendar","settings")}.forEach { app ->
    DockIcon(app.title,app.icon,app.tint){open(app.id)}
   }
   minimized.distinct().forEach { id ->
    val app=desktopApps.firstOrNull{it.id==id}
    if(app!=null) DockIcon(app.title,app.icon,app.tint){restore(id)}
   }
  }
 }
}

@Composable
private fun DockIcon(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit) {
 var pressed by remember{mutableStateOf(false)}
 val scale by animateFloatAsState(if(pressed)1.17f else 1f,label="dock-scale")
 Column(Modifier.width(43.dp).clickable { pressed=true;onClick() },horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(39.dp).shadow(5.dp,RoundedCornerShape(11.dp)).scaleCompat(scale).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(tint.copy(alpha=.95f),tint.copy(alpha=.45f)))),contentAlignment=Alignment.Center) {
   Icon(icon,null,tint=Color.White,modifier=Modifier.size(24.dp))
  }
  Spacer(Modifier.height(4.dp))
  Text(label,color=AWhite,fontSize=7.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
 }
}

private fun Modifier.scaleCompat(scale:Float):Modifier=androidx.compose.ui.draw.scale(scale)

@Composable
private fun AethraWindow(win:DesktopWindow,front:Boolean,screenW:androidx.compose.ui.unit.Dp,screenH:androidx.compose.ui.unit.Dp,wide:Boolean,
 onFocus:()->Unit,onMove:(Float,Float)->Unit,onMinimize:()->Unit,onMaximize:()->Unit,onClose:()->Unit,content:@Composable ColumnScope.()->Unit) {
 val width=if(win.maximized)screenW else if(wide)minOf(760.dp,screenW-70.dp) else screenW-20.dp
 val height=if(win.maximized)screenH else if(wide)minOf(520.dp,screenH-95.dp) else screenH-90.dp
 Column(Modifier.offset { IntOffset(if(win.maximized)0 else (win.x.dp.roundToPx()),if(win.maximized)0 else (win.y.dp.roundToPx())) }
  .width(width.coerceAtLeast(250.dp)).height(height.coerceAtLeast(190.dp))
  .shadow(if(front)25.dp else 9.dp,RoundedCornerShape(15.dp))
  .clip(RoundedCornerShape(15.dp)).background(Color(0xF01A2638))
  .border(1.dp,if(front)Color(0x70FFFFFF)else Color(0x30FFFFFF),RoundedCornerShape(15.dp))
  .clickable(onClick=onFocus),horizontalAlignment=Alignment.CenterHorizontally) {
  Row(Modifier.fillMaxWidth().height(43.dp).background(Color(0xF0263549)).pointerInput(win.id) {
   detectDragGestures(onDrag={change,delta->change.consume();onMove(delta.x/ density,delta.y/ density)})
  }.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(11.dp).clip(CircleShape).background(Color(0xFFFF625B)).clickable(onClick=onClose))
    Box(Modifier.size(11.dp).clip(CircleShape).background(Color(0xFFFFC14F)).clickable(onClick=onMinimize))
    Box(Modifier.size(11.dp).clip(CircleShape).background(Color(0xFF32C95B)).clickable(onClick=onMaximize))
   }
   Spacer(Modifier.weight(1f))
   Text(win.title,color=AWhite,fontSize=11.sp,fontWeight=FontWeight.Medium)
   Spacer(Modifier.weight(1f))
   Icon(Icons.Rounded.MoreHoriz,null,tint=ASecondary,modifier=Modifier.size(18.dp))
  }
  Column(Modifier.fillMaxSize().padding(15.dp),content=content)
 }
}

@Composable
private fun AethraLaunchpad(query:String,onQuery:(String)->Unit,apps:List<DesktopApp>,installed:List<DeviceApp>,open:(String)->Unit,launch:(DeviceApp)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0xCC071321)).clickable(onClick=onClose).padding(15.dp),contentAlignment=Alignment.Center) {
  Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).widthIn(max=760.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xE51B2A3D)).border(1.dp,AStroke,RoundedCornerShape(24.dp)).clickable(enabled=false){ }.padding(20.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Launchpad",color=AWhite,fontSize=25.sp,fontWeight=FontWeight.Light,modifier=Modifier.weight(1f))
    Icon(Icons.Rounded.Close,null,tint=AWhite,modifier=Modifier.clickable(onClick=onClose))
   }
   Spacer(Modifier.height(14.dp))
   SearchField(query,onQuery,"Search apps")
   Spacer(Modifier.height(14.dp))
   val filtered=apps.filter{query.isBlank()||it.title.contains(query,true)}
   LazyVerticalGrid(columns=GridCells.Adaptive(105.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
    items(filtered,key={it.id}) { app -> AppGridItem(app.title,app.icon,app.tint){open(app.id)} }
    items(installed.filter{query.isNotBlank()&&it.title.contains(query,true)}.take(20),key={it.packageName}) { app ->
     Column(Modifier.clickable{launch(app)}.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
      val bitmap=remember(app.packageName){app.icon.toBitmap(80,80).asImageBitmap()}
      Image(bitmap,null,Modifier.size(43.dp).clip(RoundedCornerShape(11.dp)))
      Text(app.title,color=AWhite,fontSize=9.sp,maxLines=2,textAlign=TextAlign.Center)
     }
    }
   }
  }
 }
}

@Composable
private fun AethraSpotlight(query:String,onQuery:(String)->Unit,apps:List<DesktopApp>,installed:List<DeviceApp>,open:(String)->Unit,launch:(DeviceApp)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0x99030A13)).clickable(onClick=onClose).padding(top=45.dp,horizontal=14.dp),contentAlignment=Alignment.TopCenter) {
  Column(Modifier.fillMaxWidth().widthIn(max=570.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xF0253549)).border(1.dp,AStroke,RoundedCornerShape(17.dp)).clickable(enabled=false){}.padding(15.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Rounded.Search,null,tint=ABlue)
    Spacer(Modifier.width(10.dp))
    SearchField(query,onQuery,"Spotlight Search")
    Icon(Icons.Rounded.Close,null,tint=ASecondary,modifier=Modifier.clickable(onClick=onClose))
   }
   if(query.isNotBlank()) {
    Spacer(Modifier.height(9.dp))
    apps.filter{it.title.contains(query,true)}.forEach { app ->
     Row(Modifier.fillMaxWidth().clickable{open(app.id)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
      Icon(app.icon,null,tint=app.tint);Spacer(Modifier.width(10.dp));Text(app.title,color=AWhite,fontSize=12.sp)
     }
    }
    installed.filter{it.title.contains(query,true)}.take(7).forEach { app ->
     Row(Modifier.fillMaxWidth().clickable{launch(app)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
      Icon(Icons.Rounded.Android,null,tint=ABlue);Spacer(Modifier.width(10.dp));Text(app.title,color=AWhite,fontSize=12.sp)
     }
    }
   }
  }
 }
}

@Composable
private fun SearchField(query:String,onQuery:(String)->Unit,hint:String) {
 BasicTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,textStyle=TextStyle(color=AWhite,fontSize=13.sp),decorationBox={inner->if(query.isEmpty())Text(hint,color=ASecondary,fontSize=13.sp);inner()})
}

@Composable
private fun AppGridItem(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit) {
 Column(Modifier.clip(RoundedCornerShape(13.dp)).clickable(onClick=onClick).padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(48.dp).shadow(5.dp,RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(tint,tint.copy(alpha=.55f))),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center) {
   Icon(icon,null,tint=Color.White,modifier=Modifier.size(29.dp))
  }
  Spacer(Modifier.height(5.dp));Text(title,color=AWhite,fontSize=9.sp,textAlign=TextAlign.Center,maxLines=2)
 }
}

@Composable
private fun AethraControlCenter(wifi:Boolean,onWifi:(Boolean)->Unit,bluetooth:Boolean,onBluetooth:(Boolean)->Unit,focus:Boolean,onFocus:(Boolean)->Unit,brightness:Float,onBrightness:(Float)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0x66030A14)).clickable(onClick=onClose).padding(10.dp),contentAlignment=Alignment.TopEnd) {
  Column(Modifier.widthIn(max=370.dp).fillMaxWidth(.96f).clip(RoundedCornerShape(22.dp)).background(Color(0xF0253549)).border(1.dp,AStroke,RoundedCornerShape(22.dp)).clickable(enabled=false){}.padding(17.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {Text("Control Center",color=AWhite,fontSize=22.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.Close,null,tint=AWhite,modifier=Modifier.clickable(onClick=onClose))}
   Spacer(Modifier.height(12.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    ControlTile("Wi-Fi",wifi,Icons.Rounded.Wifi,{onWifi(!wifi)},Modifier.weight(1f))
    ControlTile("Bluetooth",bluetooth,Icons.Rounded.Bluetooth,{onBluetooth(!bluetooth)},Modifier.weight(1f))
   }
   Spacer(Modifier.height(8.dp))
   ControlTile("Focus",focus,Icons.Rounded.DoNotDisturbOn,{onFocus(!focus)},Modifier.fillMaxWidth())
   Spacer(Modifier.height(13.dp));Text("DISPLAY",color=ASecondary,fontSize=9.sp,letterSpacing=1.sp)
   Slider(value=brightness,onValueChange=onBrightness)
   Text("Brightness preview only. Use Android Display settings for real hardware brightness.",color=ASecondary,fontSize=9.sp)
  }
 }
}

@Composable
private fun ControlTile(title:String,active:Boolean,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
 Row(modifier.clip(RoundedCornerShape(13.dp)).background(if(active)Color(0xFF397AA0).copy(alpha=.7f)else Color.White.copy(alpha=.09f)).clickable(onClick=onClick).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
  Icon(icon,null,tint=AWhite);Spacer(Modifier.width(9.dp));Text(title,color=AWhite,fontSize=11.sp,modifier=Modifier.weight(1f));Text(if(active)"On" else "Off",color=ASecondary,fontSize=9.sp)
 }
}

@Composable
private fun AethraNotifications(clock:Long,onClose:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 Box(Modifier.fillMaxSize().background(Color(0x77030A14)).clickable(onClick=onClose).padding(10.dp),contentAlignment=Alignment.TopEnd) {
  Column(Modifier.widthIn(max=350.dp).fillMaxWidth(.95f).clip(RoundedCornerShape(22.dp)).background(Color(0xF0253549)).padding(17.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {Text("Notification Center",color=AWhite,fontSize=21.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.Close,null,tint=AWhite,modifier=Modifier.clickable(onClick=onClose))}
   Spacer(Modifier.height(12.dp))
   Text(SimpleDateFormat("EEEE, d MMMM",locale).format(Date(clock)),color=ASecondary,fontSize=11.sp)
   Text(SimpleDateFormat("h:mm",Locale.getDefault()).format(Date(clock)),color=AWhite,fontSize=49.sp,fontWeight=FontWeight.ExtraLight)
   Spacer(Modifier.height(12.dp));Text("You’re all caught up.",color=AWhite,fontSize=14.sp)
   Text("Android notifications remain protected by the operating system.",color=ASecondary,fontSize=10.sp)
  }
 }
}

@Composable
private fun AethraMissionControl(windows:List<DesktopWindow>,workspace:Int,onWorkspace:(Int)->Unit,open:(String)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0xD9081422)).clickable(onClick=onClose).padding(15.dp),contentAlignment=Alignment.Center) {
  Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).clip(RoundedCornerShape(22.dp)).background(Color(0xE51C2B3E)).clickable(enabled=false){}.padding(18.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {Text("Mission Control",color=AWhite,fontSize=25.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.Close,null,tint=AWhite,modifier=Modifier.clickable(onClick=onClose))}
   Spacer(Modifier.height(14.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    (1..4).forEach { n ->
     Column(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if(n==workspace)Color(0xFF477EA2)else Color.White.copy(alpha=.1f)).clickable{onWorkspace(n)}.padding(10.dp)) {
      Text("Desktop $n",color=AWhite,fontSize=10.sp,fontWeight=FontWeight.Medium)
      Text(if(n==workspace)"Current" else "Switch",color=ASecondary,fontSize=8.sp)
     }
    }
   }
   Spacer(Modifier.height(18.dp));Text("OPEN WINDOWS",color=ASecondary,fontSize=9.sp,letterSpacing=1.sp)
   windows.forEach { win ->
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable{open(win.id)}.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
     Icon(desktopApps.firstOrNull{it.id==win.id}?.icon?:Icons.Rounded.Apps,null,tint=ABlue)
     Spacer(Modifier.width(10.dp));Text(win.title,color=AWhite,fontSize=11.sp,modifier=Modifier.weight(1f));Text("Switch",color=ASecondary,fontSize=9.sp)
    }
   }
   if(windows.isEmpty()) Text("No open windows",color=ASecondary,fontSize=11.sp)
  }
 }
}

@Composable
private fun FinderContent(context:Context) {
 Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
  Text("Favorites",color=ASecondary,fontSize=10.sp,letterSpacing=1.sp)
  listOf("Recents","Documents","Downloads","Pictures","Audio","Videos").forEach { name ->
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha=.06f)).clickable {
    val mime=when(name){"Pictures"->"image/*";"Audio"->"audio/*";"Videos"->"video/*";else->"*/*"}
    runCatching { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
   }.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Rounded.FolderOpen,null,tint=ABlue);Spacer(Modifier.width(10.dp));Text(name,color=AWhite,fontSize=12.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.ChevronRight,null,tint=ASecondary)
   }
  }
  Text("Your files stay in Android storage. Finder opens the system document browser for real file access.",color=ASecondary,fontSize=9.sp)
 }
}

@Composable
private fun BrowserContent(context:Context) {
 var url by rememberSaveable { mutableStateOf("https://www.apple.com") }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   BasicTextField(url,{url=it},Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha=.1f)).padding(12.dp),singleLine=true,textStyle=TextStyle(color=AWhite,fontSize=11.sp))
   Spacer(Modifier.width(7.dp))
   Box(Modifier.clip(CircleShape).background(ABlue).clickable {
    val target=if(url.startsWith("http://")||url.startsWith("https://"))url else "https://$url"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
   }.padding(11.dp)){Icon(Icons.Rounded.ArrowForward,null,tint=Color(0xFF10253A),modifier=Modifier.size(16.dp))}
  }
  Text("AETHRA Browser hands web pages to your installed Android browser. A built-in browser engine is not bundled yet.",color=ASecondary,fontSize=10.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
   listOf("Apple","Wikipedia","OpenAI").forEach { label ->
    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.1f)).clickable { url=when(label){"Apple"->"https://www.apple.com";"Wikipedia"->"https://www.wikipedia.org";else->"https://www.openai.com"} }.padding(horizontal=12.dp,vertical=8.dp)) {Text(label,color=AWhite,fontSize=9.sp)}
   }
  }
 }
}

@Composable
private fun NotesContent(notes:String,onNotes:(String)->Unit) {
 BasicTextField(notes,onNotes,Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFCF5D9)).padding(15.dp),
  textStyle=TextStyle(color=Color(0xFF282B31),fontSize=13.sp,lineHeight=20.sp))
}

@Composable
private fun CalendarContent(context:Context) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(SimpleDateFormat("MMMM yyyy",locale).format(Date()),color=AWhite,fontSize=22.sp,fontWeight=FontWeight.Light)
  Text("Your calendar stays on your device.",color=ASecondary,fontSize=10.sp)
  Box(Modifier.clip(CircleShape).background(ABlue).clickable {
   runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }.padding(horizontal=15.dp,vertical=10.dp)) {Text("Open Android Calendar",color=Color(0xFF10253A),fontSize=10.sp,fontWeight=FontWeight.Bold)}
 }
}

@Composable
private fun PhotosContent(context:Context) {
 Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
  Text("Your Library",color=AWhite,fontSize=22.sp,fontWeight=FontWeight.Light)
  Text("Choose images from your tablet.",color=ASecondary,fontSize=10.sp)
  Box(Modifier.clip(CircleShape).background(ABlue).clickable {
   runCatching { context.startActivity(Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }.padding(horizontal=16.dp,vertical=11.dp)) {Text("Choose Photos",color=Color(0xFF10253A),fontSize=10.sp,fontWeight=FontWeight.Bold)}
 }
}

@Composable
private fun SettingsContent(context:Context,wifi:Boolean,onWifi:(Boolean)->Unit,bluetooth:Boolean,onBluetooth:(Boolean)->Unit,focus:Boolean,onFocus:(Boolean)->Unit,brightness:Float,onBrightness:(Float)->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("System Settings",color=AWhite,fontSize=24.sp,fontWeight=FontWeight.Light)
  Text("AETHRA desktop preferences and Android system links.",color=ASecondary,fontSize=10.sp)
  ControlTile("Wi-Fi preview",wifi,Icons.Rounded.Wifi,{onWifi(!wifi)},Modifier.fillMaxWidth())
  ControlTile("Bluetooth preview",bluetooth,Icons.Rounded.Bluetooth,{onBluetooth(!bluetooth)},Modifier.fillMaxWidth())
  ControlTile("Focus mode preview",focus,Icons.Rounded.DoNotDisturbOn,{onFocus(!focus)},Modifier.fillMaxWidth())
  Text("Wallpaper brightness preview",color=ASecondary,fontSize=10.sp);Slider(value=brightness,onValueChange=onBrightness)
  listOf("Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,"Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,"Display" to Settings.ACTION_DISPLAY_SETTINGS,"Sound" to Settings.ACTION_SOUND_SETTINGS,"Notifications" to Settings.ACTION_APP_NOTIFICATION_SETTINGS,"Default home app" to Settings.ACTION_HOME_SETTINGS).forEach { (label,action) ->
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha=.07f)).clickable {
    runCatching {
     val intent=Intent(action)
     if(action==Settings.ACTION_APP_NOTIFICATION_SETTINGS) intent.putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
     context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
   }.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
    Text(label,color=AWhite,fontSize=11.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.OpenInNew,null,tint=ASecondary,modifier=Modifier.size(15.dp))
   }
  }
 }
}

@Composable
private fun NativeAppContent(title:String,description:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onOpen:()->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Icon(icon,null,tint=ABlue,modifier=Modifier.size(45.dp))
  Text(title,color=AWhite,fontSize=24.sp,fontWeight=FontWeight.Light)
  Text(description,color=ASecondary,fontSize=11.sp)
  Box(Modifier.clip(CircleShape).background(ABlue).clickable(onClick=onOpen).padding(horizontal=16.dp,vertical=11.dp)) {Text("Open app",color=Color(0xFF10253A),fontSize=10.sp,fontWeight=FontWeight.Bold)}
 }
}

@Composable
private fun RemindersContent(input:String,onInput:(String)->Unit,items:List<String>,onItems:(List<String>)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("Reminders",color=AWhite,fontSize=23.sp,fontWeight=FontWeight.Light)
  Row(verticalAlignment=Alignment.CenterVertically) {
   BasicTextField(input,onInput,Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha=.1f)).padding(11.dp),singleLine=true,textStyle=TextStyle(color=AWhite,fontSize=11.sp),decorationBox={inner->if(input.isEmpty())Text("Add reminder",color=ASecondary,fontSize=11.sp);inner()})
   Spacer(Modifier.width(7.dp));Text("Add",color=ABlue,fontSize=11.sp,modifier=Modifier.clickable{if(input.isNotBlank()){onItems(items+input.trim());onInput("")}})
  }
  items.forEachIndexed { index,item ->
   Row(Modifier.fillMaxWidth().clickable{onItems(items.filterIndexed{i,_->i!=index})}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Rounded.RadioButtonUnchecked,null,tint=ABlue);Spacer(Modifier.width(8.dp));Text(item,color=AWhite,fontSize=11.sp)
   }
  }
 }
}

@Composable
private fun TerminalContent() {
 Column(Modifier.fillMaxSize().background(Color(0xFF08120F)).padding(10.dp)) {
  Text("AETHRA Terminal · safe demo",color=Color(0xFF9AE6B4),fontSize=11.sp)
  Text("This is a visual terminal mock-up. It does not execute shell commands.",color=Color(0xFF9AE6B4),fontSize=9.sp)
  Text("aethra@desktop:~$ help\nAvailable demo commands: help, date, version, clear",color=Color(0xFFBCE8CA),fontSize=10.sp,lineHeight=17.sp)
 }
}

@Composable
private fun AndroidAppsContent(apps:List<DeviceApp>,launch:(DeviceApp)->Unit) {
 var query by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize()) {
  Text("Applications",color=AWhite,fontSize=22.sp,fontWeight=FontWeight.Light)
  Spacer(Modifier.height(10.dp));SearchField(query,{query=it},"Search this tablet")
  Spacer(Modifier.height(8.dp))
  LazyVerticalGrid(columns=GridCells.Adaptive(90.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   items(apps.filter{it.title.contains(query,true)},key={it.packageName}) { app ->
    Column(Modifier.clickable{launch(app)}.padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     val bitmap=remember(app.packageName){app.icon.toBitmap(80,80).asImageBitmap()}
     Image(bitmap,null,Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
     Text(app.title,color=AWhite,fontSize=9.sp,textAlign=TextAlign.Center,maxLines=2)
    }
   }
  }
 }
}
