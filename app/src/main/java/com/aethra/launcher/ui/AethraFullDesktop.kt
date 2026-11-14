@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aethra.launcher.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.provider.MediaStore
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
private data class MacOSDesktopApp(val id:String,val title:String,val icon:androidx.compose.ui.graphics.vector.ImageVector,val tint:Color)
private data class DeviceApp(val title:String,val packageName:String,val activity:String,val icon:Drawable)
private data class AudioTrack(val id:Long,val title:String,val artist:String,val album:String,val uri:Uri)
private data class DesktopWindow(val id:String,val title:String,val x:Float,val y:Float,val workspace:Int,val minimized:Boolean=false,val maximized:Boolean=false,val opening:Boolean=false)

private val desktopApps = listOf(
 MacOSDesktopApp("finder","Finder",Icons.Rounded.Folder,Color(0xFF68BDF4)),
 MacOSDesktopApp("browser","Safari",Icons.Rounded.Language,Color(0xFF5DCBE8)),
 MacOSDesktopApp("notes","Notes",Icons.Rounded.Notes,Color(0xFFFFD66B)),
 MacOSDesktopApp("calendar","Calendar",Icons.Rounded.CalendarMonth,Color(0xFFFF777C)),
 MacOSDesktopApp("photos","Photos",Icons.Rounded.PhotoLibrary,Color(0xFFCB8CF4)),
 MacOSDesktopApp("settings","System Settings",Icons.Rounded.Settings,Color(0xFFAAB8C8)),
 MacOSDesktopApp("mail","Mail",Icons.Rounded.Mail,Color(0xFF75BCEB)),
 MacOSDesktopApp("maps","Maps",Icons.Rounded.Map,Color(0xFF73C99C)),
 MacOSDesktopApp("clock","Clock",Icons.Rounded.AccessTime,Color(0xFF9EAAEA)),
 MacOSDesktopApp("contacts","Contacts",Icons.Rounded.Contacts,Color(0xFFFFA7B7)),
 MacOSDesktopApp("reminders","Reminders",Icons.Rounded.Checklist,Color(0xFFFFC56E)),
 MacOSDesktopApp("terminal","Terminal",Icons.Rounded.Code,Color(0xFF91D6B1)),
 MacOSDesktopApp("store","App Store",Icons.Rounded.Store,Color(0xFF75A9FF)),
 MacOSDesktopApp("android","Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF83D5A9)),
 MacOSDesktopApp("calculator","Calculator",Icons.Rounded.Calculate,Color(0xFFFFA94D)),
 MacOSDesktopApp("textedit","TextEdit",Icons.Rounded.EditNote,Color(0xFF82B8F4)),
 MacOSDesktopApp("preview","Preview",Icons.Rounded.PictureAsPdf,Color(0xFFB5B7C8)),
 MacOSDesktopApp("music","Music",Icons.Rounded.MusicNote,Color(0xFFE78BB8))
)

@Composable
fun MacOSDesktopSimulation27() {
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
 var notes by remember { mutableStateOf(prefs.getString("notes","Welcome to macOS 27.\n\nA new desktop, a new beginning.") ?: "") }
 var textEdit by remember { mutableStateOf(prefs.getString("textedit","") ?: "") }
 var noteInput by remember { mutableStateOf("") }
 var reminderInput by remember { mutableStateOf("") }
 var reminders by remember { mutableStateOf(prefs.getStringSet("reminders", emptySet())?.toList() ?: emptyList()) }
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
  val next=if(old==null) windows+DesktopWindow(id,desktopApps.firstOrNull{it.id==id}?.title ?: id.replaceFirstChar{it.uppercase()},if(wide)180f else 18f,if(wide)90f else 70f,workspace,opening=true)
   else { workspace=old.workspace; windows.filterNot { it.id==id } + old.copy(minimized=false,opening=true) }
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
   DesktopPhase.BOOT.name -> MacOSBoot(bootProgress)
   DesktopPhase.LOCK.name -> MacOSLockScreen(clock,battery,{ phase=DesktopPhase.DESKTOP.name },{
    runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
   })
   else -> {
    MacOSWallpaper(Modifier.fillMaxSize())
    Column(Modifier.fillMaxSize()) {
     MacOSMenuBar(clock,battery,
      { launchpad=true },
      { openApp("finder") },
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
       DesktopShortcut("Documents",Icons.Rounded.Folder,Color(0xFF79BCEB)){openApp("finder")}
      }
      windows.filter { !it.minimized && it.workspace==workspace }.forEach { win ->
       key(win.id) {
        MacOSWindow(
         win=win,front=frontWindow==win.id,screenW=maxW,screenH=maxH,wide=wide,
         onFocus={windows=windows.filterNot{it.id==win.id}+win;frontWindow=win.id},
         onMove={dx,dy->windows=windows.map{if(it.id==win.id)it.copy(x=(it.x+dx).coerceIn(0f,(maxW.value-260f).coerceAtLeast(0f)),y=(it.y+dy).coerceIn(0f,(maxH.value-170f).coerceAtLeast(0f)))else it}},
         onMinimize={windows=windows.map{if(it.id==win.id)it.copy(minimized=true)else it}},
         onMaximize={windows=windows.map{if(it.id==win.id)it.copy(maximized=!it.maximized)else it}},
         onClose={closeWindow(win.id)},
         onOpenAnimationFinished={windows=windows.map{if(it.id==win.id && it.opening)it.copy(opening=false)else it}}
        ) {
         when(win.id) {
          "finder" -> FinderContent(context)
          "browser" -> BrowserContent(context)
          "notes" -> NotesContent(notes,{notes=it;prefs.edit().putString("notes",it).apply()})
          "textedit" -> NotesContent(textEdit,{textEdit=it;prefs.edit().putString("textedit",it).apply()})
          "calculator" -> CalculatorContent()
          "preview" -> PreviewContent(context)
          "music" -> MusicContent(context)
          "calendar" -> CalendarContent(context)
          "photos" -> PhotosContent(context)
          "settings" -> SettingsContent(context,wifiPreview,{wifiPreview=it},bluetoothPreview,{bluetoothPreview=it},focusPreview,{focusPreview=it},brightness,{brightness=it})
          "mail" -> NativeAppContent("Mail","Write an email using your preferred Android mail app.",Icons.Rounded.Mail){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "maps" -> NativeAppContent("Maps","Open a map with Android's map handler.",Icons.Rounded.Map){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "clock" -> NativeAppContent("Clock","Your tablet's alarms and timers.",Icons.Rounded.AccessTime){context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "contacts" -> NativeAppContent("Contacts","Your contacts remain managed by Android.",Icons.Rounded.Contacts){context.startActivity(Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "reminders" -> RemindersContent(reminderInput,{reminderInput=it},reminders,{reminders=it;prefs.edit().putStringSet("reminders",it.toSet()).apply()})
          "terminal" -> TerminalContent()
          "store" -> NativeAppContent("App Store","Browse apps installed through your Android app store.",Icons.Rounded.Store){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.android.chrome")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
          "android" -> AndroidAppsContent(installed,::launchDeviceApp)
         }
        }
       }
      }
      Text("Desktop  $workspace / 4",color=AWhite.copy(alpha=.75f),fontSize=9.sp,
       modifier=Modifier.align(Alignment.BottomEnd).padding(15.dp))
     }
     MacOSDock(
      open={openApp(it)},
      launchpad={launchpad=true},
      minimized=windows.filter{it.minimized && it.workspace==workspace}.map{it.id},
      restore={id->windows=windows.map{if(it.id==id)it.copy(minimized=false)else it};frontWindow=id}
     )
    }
    if(launchpad) MacOSLaunchpad(search,{search=it},desktopApps,installed,::openApp,::launchDeviceApp){launchpad=false;search=""}
    if(spotlight) MacOSSpotlight(search,{search=it},desktopApps,installed,::openApp,::launchDeviceApp){spotlight=false;search=""}
    if(controlCenter) MacOSControlCenter(wifiPreview,{wifiPreview=it},bluetoothPreview,{bluetoothPreview=it},focusPreview,{focusPreview=it},brightness,{brightness=it},{controlCenter=false})
    if(notificationCenter) MacOSNotifications(clock){notificationCenter=false}
    if(missionControl) MacOSMissionControl(windows.filter{!it.minimized},workspace,{workspace=it},::openApp){missionControl=false}
   }
  }
 }
}

@Composable
private fun MacOSBoot(progress:Float) {
 val p by animateFloatAsState(progress, tween(250),label="boot")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF08090B),Color(0xFF111317),Color(0xFF050608)))),contentAlignment=Alignment.Center) {
  Column(horizontalAlignment=Alignment.CenterHorizontally) {
   GoldenGateAppleMark(Modifier.size(67.dp))
   Spacer(Modifier.height(23.dp))
   Text("macOS 27",color=AWhite,fontSize=23.sp,fontWeight=FontWeight.Light,letterSpacing=5.sp)
   Text("GOLDEN GATE",color=ASecondary,fontSize=9.sp,fontWeight=FontWeight.Medium,letterSpacing=3.4.sp)
   Spacer(Modifier.height(38.dp))
   Box(Modifier.width(158.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha=.18f))) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(p.coerceIn(0f,1f)).background(Color.White.copy(alpha=.92f)))
   }
   Spacer(Modifier.height(11.dp))
   Text("Starting up",color=ASecondary,fontSize=10.sp)
  }
 }
}

@Composable
private fun GoldenGateAppleMark(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width
  val h=size.height
  val body=Path().apply {
   moveTo(w*.51f,h*.27f)
   cubicTo(w*.42f,h*.18f,w*.25f,h*.19f,w*.17f,h*.34f)
   cubicTo(w*.06f,h*.55f,w*.20f,h*.87f,w*.37f,h*.93f)
   cubicTo(w*.45f,h*.96f,w*.48f,h*.89f,w*.55f,h*.89f)
   cubicTo(w*.66f,h*.89f,w*.70f,h*.97f,w*.78f,h*.91f)
   cubicTo(w*.94f,h*.78f,w*.96f,h*.57f,w*.85f,h*.44f)
   cubicTo(w*.80f,h*.38f,w*.73f,h*.35f,w*.67f,h*.37f)
   cubicTo(w*.60f,h*.39f,w*.57f,h*.43f,w*.51f,h*.40f)
   close()
  }
  val leaf=Path().apply {
   moveTo(w*.55f,h*.24f)
   cubicTo(w*.55f,h*.12f,w*.67f,h*.04f,w*.79f,h*.08f)
   cubicTo(w*.78f,h*.20f,w*.68f,h*.28f,w*.55f,h*.27f)
   close()
  }
  drawPath(body,Color.White)
  drawPath(leaf,Color.White)
 }
}

@Composable
private fun MacOSLockScreen(clock:Long,battery:Int,onUnlock:()->Unit,onSetHome:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 val date=remember(clock,locale){SimpleDateFormat("EEEE, d MMMM",locale).format(Date(clock))}
 val time=remember(clock){SimpleDateFormat("h:mm",locale).format(Date(clock))}
 Box(Modifier.fillMaxSize()) {
  MacOSWallpaper(Modifier.fillMaxSize())
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x35050912),Color(0x17050912),Color(0x79050912)))))
  Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
   Text(time,color=Color.White,fontSize=if(LocalConfiguration.current.screenWidthDp>=700)94.sp else 76.sp,fontWeight=FontWeight.ExtraLight,letterSpacing=(-4).sp)
   Text(date,color=Color.White.copy(alpha=.94f),fontSize=17.sp,fontWeight=FontWeight.Light)
   Spacer(Modifier.height(37.dp))
   Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFE7D3BE),Color(0xFF9E8D82))))
    .border(1.dp,Color.White.copy(alpha=.68f),CircleShape),contentAlignment=Alignment.Center) {
    Icon(Icons.Rounded.Person,null,tint=Color(0xFF3C3938),modifier=Modifier.size(43.dp))
   }
   Spacer(Modifier.height(13.dp))
   Text("Guest",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Medium)
   Text("macOS 27 Golden Gate",color=Color.White.copy(alpha=.76f),fontSize=10.sp)
   Spacer(Modifier.height(18.dp))
   Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.22f))
    .border(1.dp,Color.White.copy(alpha=.58f),CircleShape)
    .clickable(onClick=onUnlock).padding(horizontal=28.dp,vertical=12.dp)) {
    Text("Log In",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
   }
   Spacer(Modifier.height(13.dp))
   Text("Set as default desktop",color=Color.White.copy(alpha=.78f),fontSize=10.sp,
    modifier=Modifier.clickable(onClick=onSetHome))
  }
  Row(Modifier.align(Alignment.BottomCenter).padding(26.dp),verticalAlignment=Alignment.CenterVertically) {
   Icon(Icons.Rounded.BatteryFull,null,tint=Color.White.copy(alpha=.86f),modifier=Modifier.size(15.dp))
   Text("  $battery%",color=Color.White.copy(alpha=.86f),fontSize=10.sp)
  }
 }
}

@Composable
private fun MacOSWallpaper(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width
  val h=size.height
  drawRect(
   Brush.linearGradient(
    listOf(Color(0xFF74645C),Color(0xFFC7B4A1),Color(0xFFE8D9C6),Color(0xFF8C7769)),
    start=Offset(0f,0f),end=Offset(w,h)
   )
  )
  val upperFold=Path().apply {
   moveTo(-w*.12f,-h*.06f)
   cubicTo(w*.08f,h*.18f,w*.22f,h*.36f,w*.46f,h*.39f)
   cubicTo(w*.70f,h*.43f,w*.83f,h*.17f,w*1.12f,h*.13f)
   lineTo(w*1.12f,h*.34f)
   cubicTo(w*.86f,h*.38f,w*.73f,h*.63f,w*.43f,h*.56f)
   cubicTo(w*.18f,h*.50f,w*.01f,h*.27f,-w*.12f,h*.15f)
   close()
  }
  drawPath(upperFold,Brush.linearGradient(
   listOf(Color(0xFFF7EBDD),Color(0xFFE2D0BC),Color(0xFF9B8575),Color(0xFF5B4D46)),
   start=Offset(0f,h*.05f),end=Offset(w,h*.65f)
  ))
  drawPath(upperFold,Brush.linearGradient(
   listOf(Color.White.copy(alpha=.72f),Color.Transparent,Color(0xFF3D302B).copy(alpha=.38f)),
   start=Offset(0f,h*.05f),end=Offset(w,h*.45f)
  ),style=androidx.compose.ui.graphics.drawscope.Stroke(width=(h*.003f).coerceAtLeast(1.5f)))
  val lowerFold=Path().apply {
   moveTo(-w*.12f,h*.61f)
   cubicTo(w*.13f,h*.78f,w*.42f,h*.94f,w*.64f,h*.78f)
   cubicTo(w*.83f,h*.63f,w*.90f,h*.49f,w*1.12f,h*.46f)
   lineTo(w*1.12f,h*.73f)
   cubicTo(w*.91f,h*.77f,w*.83f,h*1.04f,w*.58f,h*1.06f)
   cubicTo(w*.31f,h*1.09f,w*.08f,h*.91f,-w*.12f,h*.81f)
   close()
  }
  drawPath(lowerFold,Brush.linearGradient(
   listOf(Color(0xFFEBDCCB),Color(0xFFC1A991),Color(0xFF78665B),Color(0xFF403833)),
   start=Offset(0f,h*.56f),end=Offset(w,h)
  ))
  drawPath(lowerFold,Brush.linearGradient(
   listOf(Color.White.copy(alpha=.65f),Color.Transparent,Color(0xFF382D28).copy(alpha=.4f)),
   start=Offset(0f,h*.6f),end=Offset(w,h)
  ),style=androidx.compose.ui.graphics.drawscope.Stroke(width=(h*.0025f).coerceAtLeast(1.2f)))
  val centralRibbon=Path().apply {
   moveTo(-w*.08f,h*.42f)
   cubicTo(w*.20f,h*.58f,w*.42f,h*.67f,w*.61f,h*.56f)
   cubicTo(w*.79f,h*.46f,w*.87f,h*.31f,w*1.08f,h*.30f)
   lineTo(w*1.08f,h*.43f)
   cubicTo(w*.84f,h*.44f,w*.76f,h*.62f,w*.58f,h*.70f)
   cubicTo(w*.35f,h*.80f,w*.12f,h*.61f,-w*.08f,h*.53f)
   close()
  }
  drawPath(centralRibbon,Brush.linearGradient(
   listOf(Color(0xFFFAEFE2),Color(0xFFC8B39F),Color(0xFF887366)),
   start=Offset(0f,h*.38f),end=Offset(w,h*.72f)
  ))
  drawCircle(
   Brush.radialGradient(
    listOf(Color.White.copy(alpha=.22f),Color(0xFFFFE8D1).copy(alpha=.05f),Color.Transparent),
    center=Offset(w*.78f,h*.27f),radius=w*.42f
   ),
   radius=w*.42f,center=Offset(w*.78f,h*.27f)
  )
  drawRect(Brush.verticalGradient(listOf(Color(0x14000000),Color.Transparent,Color(0x33000000))))
 }
}

@Composable
private fun MacOSMenuBar(clock:Long,battery:Int,onLaunch:()->Unit,onFinder:()->Unit,onSearch:()->Unit,onControl:()->Unit,onNotifications:()->Unit,onMission:()->Unit,onHomeSettings:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 val time=remember(clock,locale){SimpleDateFormat("EEE d MMM  h:mm",locale).format(Date(clock))}
 Row(Modifier.fillMaxWidth().height(34.dp).background(Color(0xB82D2926)).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically) {
  GoldenGateAppleMark(Modifier.size(15.dp))
  Spacer(Modifier.width(11.dp))
  Text("Finder",color=AWhite,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable(onClick=onFinder))
  Text("  File",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onFinder))
  Text("  Edit",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onFinder))
  Text("  View",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onMission))
  Text("  Go",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onFinder))
  Text("  Window",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onMission))
  Text("  Help",color=AWhite.copy(alpha=.9f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onSearch))
  Spacer(Modifier.weight(1f))
  Icon(Icons.Rounded.Search,null,tint=AWhite,modifier=Modifier.size(17.dp).clickable(onClick=onSearch))
  Spacer(Modifier.width(11.dp))
  Icon(Icons.Rounded.Tune,null,tint=AWhite,modifier=Modifier.size(17.dp).clickable(onClick=onControl))
  Spacer(Modifier.width(11.dp))
  Icon(Icons.Rounded.Notifications,null,tint=AWhite,modifier=Modifier.size(16.dp).clickable(onClick=onNotifications))
  Spacer(Modifier.width(10.dp))
  Icon(Icons.Rounded.Wifi,null,tint=AWhite,modifier=Modifier.size(15.dp))
  Spacer(Modifier.width(9.dp))
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
private fun MacOSDock(open:(String)->Unit,launchpad:()->Unit,minimized:List<String>,restore:(String)->Unit) {
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
 val iconScale= remember { Animatable(1f) }
 val scope= rememberCoroutineScope()
 Column(Modifier.width(43.dp).clickable {
  onClick()
  scope.launch {
   iconScale.animateTo(1.22f,spring(dampingRatio=0.48f,stiffness=520f))
   iconScale.animateTo(0.93f,spring(dampingRatio=0.52f,stiffness=560f))
   iconScale.animateTo(1.08f,spring(dampingRatio=0.58f,stiffness=620f))
   iconScale.animateTo(1f,spring(dampingRatio=0.62f,stiffness=680f))
  }
 },horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(39.dp).shadow(5.dp,RoundedCornerShape(11.dp)).graphicsLayer { scaleX=iconScale.value; scaleY=iconScale.value }.clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(tint.copy(alpha=.95f),tint.copy(alpha=.45f)))),contentAlignment=Alignment.Center) {
   Icon(icon,null,tint=Color.White,modifier=Modifier.size(24.dp))
  }
  Spacer(Modifier.height(4.dp))
  Text(label,color=AWhite,fontSize=7.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
 }
}


@Composable
private fun MacOSWindow(win:DesktopWindow,front:Boolean,screenW:androidx.compose.ui.unit.Dp,screenH:androidx.compose.ui.unit.Dp,wide:Boolean,
 onFocus:()->Unit,onMove:(Float,Float)->Unit,onMinimize:()->Unit,onMaximize:()->Unit,onClose:()->Unit,onOpenAnimationFinished:()->Unit,content:@Composable ColumnScope.()->Unit) {
 val windowScale by animateFloatAsState(if(win.opening)0.88f else 1f,animationSpec=spring(dampingRatio=0.78f,stiffness=420f),label="window-open-scale")
 val windowAlpha by animateFloatAsState(if(win.opening)0.72f else 1f,animationSpec=tween(190),label="window-open-alpha")
 LaunchedEffect(win.id,win.opening) { if(win.opening) { delay(16); onOpenAnimationFinished() } }
 val width=if(win.maximized)screenW else if(wide)minOf(760.dp,screenW-70.dp) else screenW-20.dp
 val height=if(win.maximized)screenH else if(wide)minOf(520.dp,screenH-95.dp) else screenH-90.dp
 Column(Modifier.offset { IntOffset(if(win.maximized)0 else (win.x.dp.roundToPx()),if(win.maximized)0 else (win.y.dp.roundToPx())) }
  .width(width.coerceAtLeast(250.dp)).height(height.coerceAtLeast(190.dp))
  .graphicsLayer { scaleX=windowScale; scaleY=windowScale; alpha=windowAlpha; transformOrigin=TransformOrigin(0.5f,0.5f) }
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
private fun MacOSLaunchpad(query:String,onQuery:(String)->Unit,apps:List<MacOSDesktopApp>,installed:List<DeviceApp>,open:(String)->Unit,launch:(DeviceApp)->Unit,onClose:()->Unit) {
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
private fun MacOSSpotlight(query:String,onQuery:(String)->Unit,apps:List<MacOSDesktopApp>,installed:List<DeviceApp>,open:(String)->Unit,launch:(DeviceApp)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0x99030A13)).clickable(onClick=onClose).padding(top=45.dp).padding(horizontal=14.dp),contentAlignment=Alignment.TopCenter) {
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
private fun MacOSControlCenter(wifi:Boolean,onWifi:(Boolean)->Unit,bluetooth:Boolean,onBluetooth:(Boolean)->Unit,focus:Boolean,onFocus:(Boolean)->Unit,brightness:Float,onBrightness:(Float)->Unit,onClose:()->Unit) {
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
private fun MacOSNotifications(clock:Long,onClose:()->Unit) {
 val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
 Box(Modifier.fillMaxSize().background(Color(0x77030A14)).clickable(onClick=onClose).padding(10.dp),contentAlignment=Alignment.TopEnd) {
  Column(Modifier.widthIn(max=350.dp).fillMaxWidth(.95f).clip(RoundedCornerShape(22.dp)).background(Color(0xF0253549)).padding(17.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {Text("Notification Center",color=AWhite,fontSize=21.sp,modifier=Modifier.weight(1f));Icon(Icons.Rounded.Close,null,tint=AWhite,modifier=Modifier.clickable(onClick=onClose))}
   Spacer(Modifier.height(12.dp))
   Text(SimpleDateFormat("EEEE, d MMMM",locale).format(Date(clock)),color=ASecondary,fontSize=11.sp)
   Text(SimpleDateFormat("h:mm",locale).format(Date(clock)),color=AWhite,fontSize=49.sp,fontWeight=FontWeight.ExtraLight)
   Spacer(Modifier.height(12.dp));Text("You’re all caught up.",color=AWhite,fontSize=14.sp)
   Text("Android notifications remain protected by the operating system.",color=ASecondary,fontSize=10.sp)
  }
 }
}

@Composable
private fun MacOSMissionControl(windows:List<DesktopWindow>,workspace:Int,onWorkspace:(Int)->Unit,open:(String)->Unit,onClose:()->Unit) {
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
 var address by rememberSaveable { mutableStateOf("") }
 var pageProgress by remember { mutableIntStateOf(0) }
 var pageTitle by remember { mutableStateOf("Start Page") }
 val webView = remember {
  WebView(context).apply {
   settings.javaScriptEnabled = true
   settings.domStorageEnabled = true
   settings.safeBrowsingEnabled = true
   settings.allowFileAccess = false
   settings.allowContentAccess = true
   settings.javaScriptCanOpenWindowsAutomatically = false
   settings.setSupportMultipleWindows(false)
   settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
   webChromeClient = object : WebChromeClient() {
    override fun onProgressChanged(view:WebView?, newProgress:Int) {
     pageProgress = newProgress
     pageTitle = view?.title ?: pageTitle
    }
   }
   webViewClient = object : WebViewClient() {
    override fun onPageStarted(view:WebView?, url:String?, favicon:android.graphics.Bitmap?) {
     if(url != null) address = url
     pageProgress = 0
    }
    override fun onPageFinished(view:WebView?, url:String?) {
     if(url != null) address = url
     pageProgress = 100
     pageTitle = view?.title ?: pageTitle
    }
    override fun shouldOverrideUrlLoading(view:WebView?, request:android.webkit.WebResourceRequest?):Boolean {
     val uri = request?.url ?: return false
     val scheme = uri.scheme?.lowercase(Locale.ROOT)
     if(scheme == "http" || scheme == "https") return false
     runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
     return true
    }
   }
  }
 }
 DisposableEffect(webView) {
  onDispose {
   webView.stopLoading()
   webView.destroy()
  }
 }
 fun navigate() {
  val raw = address.trim()
  if(raw.isBlank()) return
  val target = if(raw.contains("://")) raw else "https://$raw"
  val uri = Uri.parse(target)
  if(uri.scheme == "http" || uri.scheme == "https") {
   runCatching { webView.loadUrl(target) }
  }
 }
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   Icon(Icons.Rounded.ArrowBack,"Back",tint=if(webView.canGoBack()) AWhite else ASecondary,
    modifier=Modifier.size(19.dp).clickable(enabled=webView.canGoBack()) { webView.goBack() })
   Icon(Icons.Rounded.ArrowForward,"Forward",tint=if(webView.canGoForward()) AWhite else ASecondary,
    modifier=Modifier.size(19.dp).clickable(enabled=webView.canGoForward()) { webView.goForward() })
   Icon(Icons.Rounded.Refresh,"Reload",tint=AWhite,modifier=Modifier.size(18.dp).clickable {
    if(webView.url == null) navigate() else webView.reload()
   })
   BasicTextField(address,{address=it},Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
    .background(Color.White.copy(alpha=.11f)).padding(horizontal=10.dp,vertical=10.dp),
    singleLine=true,textStyle=TextStyle(color=AWhite,fontSize=11.sp),
    decorationBox={inner->if(address.isEmpty())Text("Search or enter website",color=ASecondary,fontSize=11.sp);inner()})
   Box(Modifier.clip(CircleShape).background(ABlue).clickable(onClick={navigate()}).padding(10.dp)) {
    Icon(Icons.Rounded.ArrowForward,null,tint=Color(0xFF10253A),modifier=Modifier.size(15.dp))
   }
  }
  if(pageProgress in 1..99) {
   Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Color.White.copy(alpha=.12f))) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(pageProgress/100f).background(ABlue))
   }
  }
  if(webView.url == null) {
   Column(Modifier.fillMaxWidth().weight(1f).padding(top=25.dp),horizontalAlignment=Alignment.CenterHorizontally,
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Icon(Icons.Rounded.Language,null,tint=ABlue,modifier=Modifier.size(48.dp))
    Text("A browser for your desktop",color=AWhite,fontSize=20.sp,fontWeight=FontWeight.Light)
    Text("Search the web or open a website. Pages render inside this window.",color=ASecondary,fontSize=10.sp,textAlign=TextAlign.Center)
    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
     listOf("Apple" to "https://www.apple.com","Wikipedia" to "https://www.wikipedia.org","OpenAI" to "https://www.openai.com").forEach { (label,url) ->
      Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.1f)).clickable { address=url;webView.loadUrl(url) }.padding(horizontal=12.dp,vertical=8.dp)) {
       Text(label,color=AWhite,fontSize=9.sp)
      }
     }
    }
   }
  } else {
   AndroidView(factory={webView},modifier=Modifier.weight(1f).fillMaxWidth())
   Text(pageTitle.ifBlank{"Web page"}+"  ·  Android System WebView",color=ASecondary,fontSize=8.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
  }
 }
}

@Composable
private fun NotesContent(notes:String,onNotes:(String)->Unit) {
 BasicTextField(notes,onNotes,Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFCF5D9)).padding(15.dp),
  textStyle=TextStyle(color=Color(0xFF282B31),fontSize=13.sp,lineHeight=20.sp))
}


@Composable
private fun CalculatorContent() {
 var display by rememberSaveable { mutableStateOf("0") }
 var stored by rememberSaveable { mutableDoubleStateOf(0.0) }
 var operation by rememberSaveable { mutableStateOf<String?>(null) }
 var replaceDisplay by rememberSaveable { mutableStateOf(true) }
 fun formatted(value:Double):String {
  if(!value.isFinite()) return "Error"
  val clean=if(value==0.0) 0.0 else value
  return if(clean % 1.0 == 0.0 && kotlin.math.abs(clean) < 1.0e12) clean.toLong().toString() else clean.toString().take(12)
 }
 fun press(key:String) {
  when(key) {
   "AC" -> {display="0";stored=0.0;operation=null;replaceDisplay=true}
   "±" -> {display=formatted(-(display.toDoubleOrNull()?:0.0));replaceDisplay=false}
   "%" -> {display=formatted((display.toDoubleOrNull()?:0.0)/100.0);replaceDisplay=false}
   "+","−","×","÷" -> {stored=display.toDoubleOrNull()?:0.0;operation=key;replaceDisplay=true}
   "=" -> {
    val right=display.toDoubleOrNull()?:0.0
    val result=when(operation) {
     "+" -> stored+right
     "−" -> stored-right
     "×" -> stored*right
     "÷" -> if(right==0.0) Double.NaN else stored/right
     else -> right
    }
    display=formatted(result);operation=null;replaceDisplay=true
   }
   else -> {
    if(key=="." && (if(replaceDisplay) "" else display).contains(".")) return
    display=if(replaceDisplay) {if(key==".")"0." else key} else if(display=="0" && key!=".") key else display+key
    replaceDisplay=false
   }
  }
 }
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("Calculator",color=AWhite,fontSize=14.sp)
  Box(Modifier.fillMaxWidth().weight(.8f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF111A27)).padding(16.dp),contentAlignment=Alignment.BottomEnd) {
   Text(display,color=AWhite,fontSize=38.sp,fontWeight=FontWeight.Light,maxLines=1)
  }
  val keys=listOf(
   listOf("AC","±","%","÷"),
   listOf("7","8","9","×"),
   listOf("4","5","6","−"),
   listOf("1","2","3","+"),
   listOf("0",".","=","=")
  )
  keys.forEachIndexed { rowIndex,row ->
   Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    row.forEachIndexed { colIndex,key ->
     if(rowIndex==4 && colIndex==3) Spacer(Modifier.weight(1f))
     else Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(13.dp))
      .background(when {key in listOf("÷","×","−","+","=")->Color(0xFF397AA0);key=="AC"->Color(0xFF465366);else->Color(0xFF27374B)})
      .clickable { press(key) },contentAlignment=Alignment.Center) {
      Text(key,color=AWhite,fontSize=if(key.length>1)14.sp else 19.sp,fontWeight=FontWeight.Medium)
     }
    }
   }
  }
 }
}


@Composable
private fun MusicContent(context:Context) {
 val permission=if(android.os.Build.VERSION.SDK_INT>=33) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
 var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,permission)==android.content.pm.PackageManager.PERMISSION_GRANTED) }
 var tracks by remember { mutableStateOf(emptyList<AudioTrack>()) }
 var current by remember { mutableStateOf<AudioTrack?>(null) }
 var playing by remember { mutableStateOf(false) }
 var loading by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var player by remember { mutableStateOf<MediaPlayer?>(null) }
 val requestPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  hasPermission=granted
  if(!granted) error="Music access was not granted. You can still choose an audio file from Android."
 }
 fun stopPlayback() {
  runCatching { player?.stop() }
  runCatching { player?.release() }
  player=null
  playing=false
  current=null
 }
 fun play(track:AudioTrack) {
  stopPlayback()
  loading=true
  error=null
  runCatching {
   val next=MediaPlayer()
   player=next
   next.setOnPreparedListener { media -> loading=false;media.start();playing=true }
   next.setOnCompletionListener { playing=false;loading=false }
   next.setOnErrorListener { _,_,_ -> loading=false;playing=false;error="This audio file could not be played.";true }
   current=track
   next.setDataSource(context,track.uri)
   next.prepareAsync()
  }.onFailure {
   loading=false
   playing=false
   error="Unable to open this audio file."
   runCatching { player?.release() }
   player=null
  }
 }
 DisposableEffect(Unit) {
  onDispose { runCatching { player?.release() };player=null }
 }
 LaunchedEffect(hasPermission) {
  if(hasPermission) {
   tracks=withContext(Dispatchers.IO) {
    val result=mutableListOf<AudioTrack>()
    val collection=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection=arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM)
    runCatching {
     context.contentResolver.query(collection,projection,
      "\${MediaStore.Audio.Media.IS_MUSIC} != 0",null,
      "\${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
      val idCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val titleCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
      val artistCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      val albumCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
      while(cursor.moveToNext()) {
       val id=cursor.getLong(idCol)
       val title=cursor.getString(titleCol)?.takeIf{it.isNotBlank()} ?: "Unknown track"
       val artist=cursor.getString(artistCol)?.takeIf{it.isNotBlank()} ?: "Unknown artist"
       val album=cursor.getString(albumCol)?.takeIf{it.isNotBlank()} ?: "Unknown album"
       result.add(AudioTrack(id,title,artist,album,Uri.withAppendedPath(collection,id.toString())))
      }
     }
    }
    result
   }
  }
 }
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   Column(Modifier.weight(1f)) {
    Text("Music",color=AWhite,fontSize=24.sp,fontWeight=FontWeight.Light)
    Text(if(hasPermission) "\${tracks.size} songs on this tablet" else "Your music library",color=ASecondary,fontSize=10.sp)
   }
   Icon(Icons.Rounded.Refresh,"Refresh",tint=ABlue,modifier=Modifier.size(21.dp).clickable {
    if(hasPermission) { hasPermission=false;hasPermission=true } else requestPermission.launch(permission)
   })
  }
  if(!hasPermission) {
   Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
    Icon(Icons.Rounded.LibraryMusic,null,tint=ABlue,modifier=Modifier.size(49.dp))
    Text("Your music, in one place",color=AWhite,fontSize=17.sp)
    Text("Allow audio access to browse music stored on this tablet.",color=ASecondary,fontSize=10.sp,textAlign=TextAlign.Center)
    Box(Modifier.clip(CircleShape).background(ABlue).clickable { requestPermission.launch(permission) }.padding(horizontal=18.dp,vertical=11.dp)) {
     Text("Allow Music Access",color=Color(0xFF10253A),fontSize=10.sp,fontWeight=FontWeight.Bold)
    }
   }
  } else {
   if(current!=null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha=.1f)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
     Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF8C4E79)),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.MusicNote,null,tint=AWhite) }
     Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
      Text(current!!.title,color=AWhite,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
      Text(current!!.artist,color=ASecondary,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
     }
     if(loading) CircularProgressIndicatorCompat()
     else Icon(if(playing)Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,null,tint=ABlue,modifier=Modifier.size(31.dp).clickable {
      if(playing) { runCatching { player?.pause() };playing=false } else runCatching { player?.start();playing=true }
     })
     Icon(Icons.Rounded.StopCircle,null,tint=ASecondary,modifier=Modifier.padding(start=7.dp).size(25.dp).clickable { stopPlayback() })
    }
   }
   if(error!=null) Text(error!!,color=Color(0xFFFFB4A9),fontSize=9.sp)
   if(tracks.isEmpty()) {
    Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
     Icon(Icons.Rounded.MusicOff,null,tint=ASecondary,modifier=Modifier.size(38.dp))
     Text("No music found",color=AWhite,fontSize=15.sp)
     Text("Add audio files to your device and refresh.",color=ASecondary,fontSize=10.sp)
    }
   } else {
    LazyVerticalGrid(columns=GridCells.Adaptive(220.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
     items(tracks,key={it.id}) { track ->
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(if(current?.id==track.id)Color(0xFF315A75)else Color.White.copy(alpha=.055f)).clickable { play(track) }.padding(9.dp),verticalAlignment=Alignment.CenterVertically) {
       Icon(Icons.Rounded.MusicNote,null,tint=if(current?.id==track.id)ABlue else ASecondary,modifier=Modifier.size(22.dp))
       Column(Modifier.weight(1f).padding(start=9.dp)) {
        Text(track.title,color=AWhite,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(track.artist+" · "+track.album,color=ASecondary,fontSize=8.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
       }
       if(current?.id==track.id && playing) Icon(Icons.Rounded.Equalizer,null,tint=ABlue,modifier=Modifier.size(17.dp))
      }
     }
    }
   }
  }
 }
}

@Composable
private fun CircularProgressIndicatorCompat() {
 androidx.compose.material3.CircularProgressIndicator(modifier=Modifier.size(22.dp),color=ABlue,strokeWidth=2.dp)
}

@Composable
private fun PreviewContent(context:Context) {
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Icon(Icons.Rounded.PictureAsPdf,null,tint=ABlue,modifier=Modifier.size(48.dp))
  Text("Preview",color=AWhite,fontSize=24.sp,fontWeight=FontWeight.Light)
  Text("Open a PDF, image, or document with an Android viewer. The selected file stays in its original location.",color=ASecondary,fontSize=11.sp,lineHeight=16.sp)
  Box(Modifier.clip(CircleShape).background(ABlue).clickable {
   runCatching {
    context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
     .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   }
  }.padding(horizontal=17.dp,vertical=11.dp)) {
   Text("Open File",color=Color(0xFF10253A),fontSize=10.sp,fontWeight=FontWeight.Bold)
  }
 }
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
  Text("macOS 27 desktop preferences and Android system links.",color=ASecondary,fontSize=10.sp)
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
  Text("macOS 27 Terminal · safe demo",color=Color(0xFF9AE6B4),fontSize=11.sp)
  Text("This is a visual terminal mock-up. It does not execute shell commands.",color=Color(0xFF9AE6B4),fontSize=9.sp)
  Text("user@macos27:~$ help\nAvailable demo commands: help, date, version, clear",color=Color(0xFFBCE8CA),fontSize=10.sp,lineHeight=17.sp)
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
