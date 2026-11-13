@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aethra.launcher.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SW = Color(0xFFF7FAFF)
private val SM = Color(0xFFB8C7D8)
private val SB = Color(0xFF79C9F2)
private data class SimApp(val id:String,val name:String,val icon:androidx.compose.ui.graphics.vector.ImageVector,val tint:Color)
private data class AndroidAppItem(val name:String,val packageName:String,val activity:String,val icon:Drawable)

private val simApps = listOf(
 SimApp("finder","Finder",Icons.Rounded.Folder,Color(0xFF65B9F4)),
 SimApp("safari","Safari",Icons.Rounded.Language,Color(0xFF67C7E8)),
 SimApp("notes","Notes",Icons.Rounded.Notes,Color(0xFFFFD66B)),
 SimApp("calendar","Calendar",Icons.Rounded.CalendarMonth,Color(0xFFFF777C)),
 SimApp("photos","Photos",Icons.Rounded.PhotoLibrary,Color(0xFFCB8CF4)),
 SimApp("terminal","Terminal",Icons.Rounded.Code,Color(0xFF91D6B1)),
 SimApp("store","App Store",Icons.Rounded.Store,Color(0xFF75A9FF)),
 SimApp("settings","Settings",Icons.Rounded.Settings,Color(0xFFAAB8C8))
)

@Composable
fun MacSimulatorApp() {
 val context=LocalContext.current
 val wide=LocalConfiguration.current.screenWidthDp>=700
 var boot by remember { mutableStateOf(true) }
 var progress by remember { mutableFloatStateOf(0f) }
 var bootText by remember { mutableStateOf("Starting AETHRA system") }
 var active by remember { mutableStateOf<String?>(null) }
 var launchpad by remember { mutableStateOf(false) }
 var androidPanel by remember { mutableStateOf(false) }
 var query by remember { mutableStateOf("") }
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 var battery by remember { mutableIntStateOf(0) }
 var installed by remember { mutableStateOf(emptyList<AndroidAppItem>()) }
 var notes by rememberSaveable { mutableStateOf("Welcome to AETHRA Notes.\n\nA fresh space for your ideas.") }
 var address by rememberSaveable { mutableStateOf("https://www.apple.com") }
 var terminal by rememberSaveable { mutableStateOf("AETHRA Terminal [simulated]\nType help to see demo commands.") }
 var command by rememberSaveable { mutableStateOf("") }
 var finder by remember { mutableStateOf("Recents") }
 var wifi by rememberSaveable { mutableStateOf(true) }
 var dark by rememberSaveable { mutableStateOf(true) }

 LaunchedEffect(Unit) {
  val labels=listOf("Starting AETHRA system","Loading Golden Gate desktop","Preparing applications","Almost there")
  for(i in 1..100) {
   progress=i/100f
   bootText=when { i<25->labels[0]; i<52->labels[1]; i<83->labels[2]; else->labels[3] }
   delay(35)
  }
  delay(350); boot=false
 }
 LaunchedEffect(Unit) {
  while(true) {
   now=System.currentTimeMillis()
   val bm=context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
   battery=bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0,100)?:0
   delay(30000)
  }
 }
 LaunchedEffect(Unit) {
  installed=withContext(Dispatchers.IO) {
   @Suppress("DEPRECATION")
   context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
    .mapNotNull { info ->
     val a=info.activityInfo?:return@mapNotNull null
     if(a.packageName==context.packageName)return@mapNotNull null
     AndroidAppItem(info.loadLabel(context.packageManager).toString(),a.packageName,a.name,info.loadIcon(context.packageManager))
    }.distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.getDefault()) }
  }
 }
 Box(Modifier.fillMaxSize().background(Color(0xFF071727))) {
  SimWallpaper(Modifier.fillMaxSize())
  AnimatedVisibility(boot,Modifier.fillMaxSize()) { BootAnimation(progress,bootText) }
  AnimatedVisibility(!boot,Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=if(wide)22.dp else 10.dp, vertical=8.dp)) {
    SimMenu(now,battery,{launchpad=true},{active="settings"})
    Spacer(Modifier.height(12.dp))
    Box(Modifier.weight(1f).fillMaxWidth()) {
     if(wide) {
      Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
       Column(Modifier.width(205.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SimWidget(now)
        GlassPanel(Modifier.fillMaxWidth(),18.dp,14.dp) {
         Text("GOLDEN GATE",color=SM,fontSize=9.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold)
         Spacer(Modifier.height(7.dp))
         Text("Your own little\ncorner of the world.",color=SW,fontSize=21.sp,lineHeight=25.sp,fontWeight=FontWeight.Light)
         Spacer(Modifier.height(9.dp))
         Text("AETHRA  •  SIMULATOR",color=SM,fontSize=8.sp,letterSpacing=1.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("A simulated desktop experience",color=SM,fontSize=9.sp)
       }
       Column(Modifier.weight(1f)) {
        Text("Welcome back.",color=SW,fontSize=28.sp,fontWeight=FontWeight.Light)
        Text("Choose an app to begin.",color=SM,fontSize=11.sp)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns=GridCells.Adaptive(94.dp),modifier=Modifier.weight(1f),
         horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items(simApps,key={it.id}) { SimIcon(it){active=it.id} }
         item { SimIcon(SimApp("android","Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF86D9B0))){androidPanel=true} }
        }
       }
      }
     } else {
      Column(Modifier.fillMaxSize()) {
       Text("Golden Gate",color=SW,fontSize=31.sp,fontWeight=FontWeight.Light)
       Text("Welcome to your desktop.",color=SM,fontSize=12.sp)
       Spacer(Modifier.height(16.dp))
       LazyVerticalGrid(columns=GridCells.Adaptive(78.dp),modifier=Modifier.weight(1f),
        horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        items(simApps,key={it.id}) { SimIcon(it){active=it.id} }
        item { SimIcon(SimApp("android","Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF86D9B0))){androidPanel=true} }
       }
      }
     }
     AnimatedVisibility(active!=null,Modifier.fillMaxSize()) {
      active?.let { id ->
       SimWindow(id,wide,notes,{notes=it},address,{address=it},terminal,command,{terminal=it},{command=it},
        finder,{finder=it},wifi,{wifi=it},dark,{dark=it},{active=null},{androidPanel=true},context)
      }
     }
     AnimatedVisibility(launchpad,Modifier.fillMaxSize()) {
      SimLaunchpad(query,{query=it},{id->if(id=="android")androidPanel=true else active=id;launchpad=false;query=""},{launchpad=false})
     }
     AnimatedVisibility(androidPanel,Modifier.fillMaxSize()) {
      AndroidAppsWindow(installed,{androidPanel=false}) { app ->
       runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(app.packageName,app.activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
      }
     }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
     SimDock({launchpad=true},{active=it},{androidPanel=true})
    }
   }
  }
 }
}

@Composable
private fun BootAnimation(progress:Float,line:String) {
 val glow by animateFloatAsState(if(progress>.1f)1f else .3f,tween(1200),label="boot-glow")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF071522),Color(0xFF123B52),Color(0xFF071522)))),contentAlignment=Alignment.Center) {
  Box(Modifier.size(310.dp).background(Brush.radialGradient(listOf(Color(0xFF69C9EE).copy(alpha=.18f*glow),Color.Transparent)),CircleShape))
  Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(30.dp)) {
   Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFB58D),Color(0xFFDB6E65),Color(0xFF6DAAC1)))).border(1.dp,Color.White.copy(alpha=.5f),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center) {
    Text("A",color=Color.White,fontSize=57.sp,fontWeight=FontWeight.Light)
   }
   Spacer(Modifier.height(24.dp))
   Text("AETHRA",color=SW,fontSize=24.sp,fontWeight=FontWeight.SemiBold,letterSpacing=5.sp)
   Text("GOLDEN GATE",color=SM,fontSize=9.sp,letterSpacing=3.sp)
   Spacer(Modifier.height(42.dp))
   Box(Modifier.width(190.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha=.15f))) {
    Box(Modifier.fillMaxWidth(progress.coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(SB,Color(0xFFFFB58D)))))
   }
   Spacer(Modifier.height(12.dp))
   Text(line,color=SM,fontSize=10.sp)
  }
 }
}

@Composable
private fun SimWallpaper(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width; val h=size.height
  drawRect(Brush.verticalGradient(0f to Color(0xFF061727),.35f to Color(0xFF174B64),.67f to Color(0xFF718F99),1f to Color(0xFFB99B84),endY=h))
  val sun=androidx.compose.ui.geometry.Offset(w*.79f,h*.25f)
  drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC995).copy(alpha=.4f),Color.Transparent),sun,w*.25f),w*.25f,sun)
  drawCircle(Color(0xFFFFD5A7).copy(alpha=.8f),w*.04f,sun)
  val land=Path().apply { moveTo(0f,h*.57f);cubicTo(w*.18f,h*.46f,w*.3f,h*.52f,w*.48f,h*.54f);cubicTo(w*.65f,h*.48f,w*.78f,h*.53f,w,h*.59f);lineTo(w,h);lineTo(0f,h);close() }
  drawPath(land,Color(0xFF102F42).copy(alpha=.7f))
  val x1=w*.29f;val x2=w*.72f;val top=h*.34f;val deck=h*.70f
  val cable=Path().apply { moveTo(w*.02f,h*.66f);quadraticTo(x1,h*.37f,x2,h*.66f);quadraticTo(w*.86f,h*.7f,w*.98f,h*.69f) }
  drawPath(cable,Color(0xFFFF9679).copy(alpha=.85f),style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(),cap=StrokeCap.Round))
  listOf(x1,x2).forEach { x -> drawLine(Color(0xFFFF9679),androidx.compose.ui.geometry.Offset(x,top),androidx.compose.ui.geometry.Offset(x,deck),4.dp.toPx()) }
  for(i in 1..18) { val x=x1+(x2-x1)*i/19f;val t=((x-x1)/(x2-x1))*2f-1f;val y=h*.65f-h*.27f*(1f-t*t);drawLine(Color(0xFFFF9679).copy(alpha=.48f),androidx.compose.ui.geometry.Offset(x,y),androidx.compose.ui.geometry.Offset(x,deck),1.dp.toPx()) }
  drawLine(Color(0xFFFF9679),androidx.compose.ui.geometry.Offset(w*.02f,deck),androidx.compose.ui.geometry.Offset(w*.98f,deck),2.dp.toPx())
 }
}

@Composable
private fun SimMenu(now:Long,battery:Int,onLaunch:()->Unit,onSettings:()->Unit) {
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.24f),Color.White.copy(alpha=.09f)))).border(1.dp,Color.White.copy(alpha=.24f),RoundedCornerShape(18.dp)).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
  Text("A",color=SW,fontWeight=FontWeight.Black,fontSize=16.sp,modifier=Modifier.clip(CircleShape).background(Color.White.copy(alpha=.16f)).clickable(onClick=onLaunch).padding(horizontal=8.dp,vertical=3.dp))
  Spacer(Modifier.width(10.dp));Text("AETHRA",color=SW,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
  Spacer(Modifier.width(12.dp));Text("Desktop",color=SW.copy(alpha=.8f),fontSize=10.sp,modifier=Modifier.clickable(onClick=onLaunch))
  Spacer(Modifier.weight(1f));Icon(Icons.Rounded.Wifi,null,tint=SW,modifier=Modifier.size(14.dp))
  Spacer(Modifier.width(7.dp));Icon(Icons.Rounded.BatteryFull,null,tint=SW,modifier=Modifier.size(14.dp))
  Text("$battery%",color=SW,fontSize=9.sp);Spacer(Modifier.width(10.dp))
  Text(SimpleDateFormat("EEE d MMM  h:mm a",LocalConfiguration.current.locales[0]).format(Date(now)),color=SW,fontSize=9.sp)
  Spacer(Modifier.width(7.dp));Icon(Icons.Rounded.Settings,"Settings",tint=SW,modifier=Modifier.size(17.dp).clickable(onClick=onSettings))
 }
}

@Composable
private fun SimWidget(now:Long) {
 GlassPanel(Modifier.fillMaxWidth(),19.dp,14.dp) {
  Icon(Icons.Rounded.CalendarMonth,null,tint=Color(0xFFFFC995),modifier=Modifier.size(19.dp))
  Text(SimpleDateFormat("EEEE",LocalConfiguration.current.locales[0]).format(Date(now)),color=SM,fontSize=10.sp)
  Text(SimpleDateFormat("d",LocalConfiguration.current.locales[0]).format(Date(now)),color=SW,fontSize=44.sp,fontWeight=FontWeight.Light)
  Text(SimpleDateFormat("MMMM yyyy",LocalConfiguration.current.locales[0]).format(Date(now)),color=SM,fontSize=10.sp)
 }
}

@Composable
private fun SimIcon(app:SimApp,onClick:()->Unit) {
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick=onClick).padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Box(Modifier.size(54.dp).shadow(8.dp,RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(app.tint,app.tint.copy(alpha=.48f)))),contentAlignment=Alignment.Center) {
   Icon(app.icon,null,tint=Color.White,modifier=Modifier.size(29.dp))
  }
  Text(app.name,color=SW,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
 }
}

@Composable
private fun SimDock(onLaunchpad:()->Unit,onOpen:(String)->Unit,onAndroid:()->Unit) {
 Row(Modifier.clip(RoundedCornerShape(23.dp)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.25f),Color(0xC9162B40)))).border(1.dp,Color.White.copy(alpha=.3f),RoundedCornerShape(23.dp)).padding(horizontal=9.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
  simApps.take(5).forEach { DockButton(it.name,it.icon,it.tint){onOpen(it.id)} }
  Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha=.3f)))
  DockButton("Launchpad",Icons.Rounded.Apps,Color(0xFF8CBDF4),onLaunchpad)
  DockButton("Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF83D5A9),onAndroid)
 }
}
@Composable
private fun DockButton(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit) {
 Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha=.28f)).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
  Icon(icon,label,tint=Color.White,modifier=Modifier.size(22.dp))
 }
}
@Composable
private fun GlassPanel(modifier:Modifier=Modifier,radius:androidx.compose.ui.unit.Dp=22.dp,padding:androidx.compose.ui.unit.Dp=14.dp,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.shadow(16.dp,RoundedCornerShape(radius)).clip(RoundedCornerShape(radius)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.23f),Color(0xB91A2D43),Color.White.copy(alpha=.07f)))).border(1.dp,Color.White.copy(alpha=.25f),RoundedCornerShape(radius)).padding(padding),content=content)
}

@Composable
private fun SimLaunchpad(query:String,onQuery:(String)->Unit,onOpen:(String)->Unit,onClose:()->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0xD9071422)).padding(10.dp),contentAlignment=Alignment.Center) {
  GlassPanel(Modifier.fillMaxWidth().fillMaxHeight(.94f),27.dp,16.dp) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("Launchpad",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text("YOUR APPLICATIONS",color=SM,fontSize=9.sp,letterSpacing=1.5.sp) }
    Icon(Icons.Rounded.Close,"Close",tint=SW,modifier=Modifier.size(25.dp).clickable(onClick=onClose))
   }
   Spacer(Modifier.height(13.dp))
   BasicTextField(query,onQuery,Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha=.13f)).padding(13.dp),singleLine=true,textStyle=TextStyle(color=SW,fontSize=13.sp),decorationBox={ inner->if(query.isEmpty())Text("Search applications",color=SM,fontSize=13.sp);inner() })
   Spacer(Modifier.height(12.dp))
   LazyVerticalGrid(columns=GridCells.Adaptive(84.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    items(simApps.filter { it.name.contains(query,true) || query.isBlank() },key={it.id}) { SimIcon(it){onOpen(it.id)} }
    if("android apps".contains(query,true)||query.isBlank()) item { SimIcon(SimApp("android","Android Apps",Icons.Rounded.PhoneAndroid,Color(0xFF86D9B0))){onOpen("android")} }
   }
  }
 }
}

@Composable
private fun AndroidAppsWindow(apps:List<AndroidAppItem>,onClose:()->Unit,onLaunch:(AndroidAppItem)->Unit) {
 Box(Modifier.fillMaxSize().background(Color(0xD9071422)).padding(10.dp),contentAlignment=Alignment.Center) {
  GlassPanel(Modifier.fillMaxWidth().fillMaxHeight(.94f),26.dp,15.dp) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("Android Apps",color=SW,fontSize=23.sp,fontWeight=FontWeight.Light);Text(apps.size.toString()+" INSTALLED APPS ON THIS DEVICE",color=SM,fontSize=9.sp,letterSpacing=1.sp) }
    Icon(Icons.Rounded.Close,"Close",tint=SW,modifier=Modifier.size(25.dp).clickable(onClick=onClose))
   }
   Spacer(Modifier.height(12.dp))
   LazyVerticalGrid(columns=GridCells.Adaptive(88.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    items(apps,key={it.packageName}) { app ->
     val bitmap=remember(app.packageName){app.icon.toBitmap(112,112).asImageBitmap()}
     Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{onLaunch(app)}.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
      Image(bitmap,app.name,Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)))
      Text(app.name,color=SW,fontSize=9.sp,maxLines=2,textAlign=TextAlign.Center,overflow=TextOverflow.Ellipsis)
     }
    }
   }
  }
 }
}

@Composable
private fun SimWindow(id:String,wide:Boolean,notes:String,onNotes:(String)->Unit,address:String,onAddress:(String)->Unit,terminal:String,command:String,onTerminal:(String)->Unit,onCommand:(String)->Unit,finder:String,onFinder:(String)->Unit,wifi:Boolean,onWifi:(Boolean)->Unit,dark:Boolean,onDark:(Boolean)->Unit,onClose:()->Unit,onAndroid:()->Unit,context:Context) {
 val app=simApps.firstOrNull{it.id==id}
 val title=app?.name?:"AETHRA"
 Box(Modifier.fillMaxSize().background(Color(0x66020A12)).padding(if(wide)26.dp else 7.dp),contentAlignment=Alignment.Center) {
  GlassPanel(Modifier.fillMaxWidth().fillMaxHeight(if(wide).91f else .94f),24.dp,0.dp) {
   Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
     listOf(Color(0xFFFF6F68),Color(0xFFFFC65E),Color(0xFF69D99A)).forEach { c->Box(Modifier.size(11.dp).clip(CircleShape).background(c).clickable{if(c==Color(0xFFFF6F68))onClose()}) }
    }
    Spacer(Modifier.width(12.dp));Text(title,color=SW,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
    Spacer(Modifier.weight(1f));Text("AETHRA SIMULATOR",color=SM,fontSize=8.sp,letterSpacing=1.sp)
   }
   Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=.13f)))
   Column(Modifier.fillMaxSize().padding(15.dp).verticalScroll(rememberScrollState())) {
    when(id) {
     "finder"->FinderApp(finder,onFinder)
     "safari"->SafariApp(address,onAddress,context)
     "notes"->NotesApp(notes,onNotes)
     "calendar"->CalendarApp()
     "photos"->PhotosApp()
     "terminal"->TerminalApp(command,onCommand,terminal,onTerminal)
     "store"->StoreApp()
     "settings"->SettingsApp(wifi,onWifi,dark,onDark,onAndroid)
     else->Text("Welcome to AETHRA.",color=SW)
    }
   }
  }
 }
}

@Composable
private fun FinderApp(folder:String,onFolder:(String)->Unit) {
 Text("Favourites",color=SM,fontSize=10.sp,letterSpacing=1.sp);Spacer(Modifier.height(10.dp))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
  listOf("Recents","Apps","Desktop","Documents","Downloads").forEach { n->Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if(folder==n)Color.White.copy(alpha=.2f)else Color.White.copy(alpha=.08f)).clickable{onFolder(n)}.padding(vertical=12.dp),contentAlignment=Alignment.Center){Text(n,color=SW,fontSize=9.sp)} }
 }
 Spacer(Modifier.height(20.dp));Text(folder,color=SW,fontSize=23.sp,fontWeight=FontWeight.Light)
 val rows=when(folder){"Apps"->simApps.map{it.name};"Desktop"->listOf("Golden Gate Wallpaper","Welcome");"Documents"->listOf("Welcome to AETHRA.txt","Project Notes.txt");"Downloads"->listOf("No recent downloads");else->listOf("Golden Gate Wallpaper","Welcome to AETHRA.txt","Applications")}
 rows.forEach { Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.InsertDriveFile,null,tint=SB,modifier=Modifier.size(19.dp));Spacer(Modifier.width(10.dp));Text(it,color=SW,fontSize=12.sp)} }
 Text("Simulated Finder. Your Android files remain in Android storage.",color=SM,fontSize=10.sp)
}

@Composable
private fun SafariApp(address:String,onAddress:(String)->Unit,context:Context) {
 Text("Safari",color=SW,fontSize=23.sp,fontWeight=FontWeight.Light);Spacer(Modifier.height(10.dp))
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
  BasicTextField(address,onAddress,Modifier.weight(1f).clip(CircleShape).background(Color.White.copy(alpha=.12f)).padding(12.dp),singleLine=true,textStyle=TextStyle(color=SW,fontSize=12.sp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Go))
  Box(Modifier.clip(CircleShape).background(SB).clickable{val u=if(address.startsWith("http"))address else "https://"+address;runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)))}}.padding(horizontal=16.dp,vertical=11.dp)){Text("Open",color=Color(0xFF09243A),fontSize=11.sp,fontWeight=FontWeight.Bold)}
 }
 Spacer(Modifier.height(18.dp));Text("A window to the web.",color=SW,fontSize=20.sp,fontWeight=FontWeight.Light)
 Text("Enter a website above. Pages open in your Android browser; this simulator does not bundle a separate web engine.",color=SM,fontSize=12.sp,lineHeight=18.sp)
 listOf("Apple" to "https://www.apple.com","Wikipedia" to "https://www.wikipedia.org","OpenAI" to "https://www.openai.com","GitHub" to "https://github.com").forEach{(n,u)->Text(n,color=SB,fontSize=13.sp,modifier=Modifier.clickable{onAddress(u)}.padding(vertical=7.dp))}
}

@Composable
private fun NotesApp(notes:String,onNotes:(String)->Unit) {
 Text("Notes",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text("Your notes stay in this simulator session.",color=SM,fontSize=10.sp);Spacer(Modifier.height(12.dp))
 BasicTextField(notes,onNotes,Modifier.fillMaxWidth().heightIn(min=260.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha=.08f)).padding(14.dp),textStyle=TextStyle(color=SW,fontSize=14.sp,lineHeight=21.sp))
}
@Composable
private fun CalendarApp() {
 val today=java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
 Text("Calendar",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text(SimpleDateFormat("MMMM yyyy",LocalConfiguration.current.locales[0]).format(Date()),color=SM,fontSize=12.sp);Spacer(Modifier.height(12.dp))
 LazyVerticalGrid(columns=GridCells.Fixed(7),modifier=Modifier.height(230.dp)){items((1..31).toList()){d->Box(Modifier.padding(3.dp).aspectRatio(1f).clip(CircleShape).background(if(d==today)SB else Color.White.copy(alpha=.08f)),contentAlignment=Alignment.Center){Text(d.toString(),color=if(d==today)Color(0xFF09243A)else SW,fontSize=11.sp)}}}
 Text("Calendar preview. Android calendar events are not modified.",color=SM,fontSize=10.sp)
}
@Composable
private fun PhotosApp() {
 Text("Photos",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text("A little gallery of the Golden Gate atmosphere.",color=SM,fontSize=11.sp);Spacer(Modifier.height(14.dp))
 LazyVerticalGrid(columns=GridCells.Adaptive(130.dp),modifier=Modifier.heightIn(min=220.dp,max=390.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
  items((1..6).toList()){i->Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF164B65),Color(0xFFFFA47F).copy(alpha=.8f),Color(0xFF0C2438))),),contentAlignment=Alignment.BottomStart){Text("Golden Gate 0"+i,color=Color.White,fontSize=10.sp,modifier=Modifier.padding(9.dp))}}
 }
}
@Composable
private fun TerminalApp(input:String,onInput:(String)->Unit,output:String,onOutput:(String)->Unit) {
 Text("Terminal",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light)
 Text(output,color=Color(0xFF9BE2B5),fontSize=11.sp,lineHeight=17.sp,modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xE9081118)).padding(13.dp))
 Spacer(Modifier.height(9.dp))
 Row(verticalAlignment=Alignment.CenterVertically) {
  Text("aethra %",color=Color(0xFF9BE2B5),fontSize=11.sp);Spacer(Modifier.width(7.dp))
  BasicTextField(input,onInput,Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=SW,fontSize=12.sp),decorationBox={inner->if(input.isEmpty())Text("help",color=SM,fontSize=12.sp);inner()})
  Text("Run",color=SB,fontSize=11.sp,modifier=Modifier.clickable{
   val cmd=input.trim().lowercase()
   val result=when(cmd){"help"->"Demo commands: help, date, apps, clear, about";"date"->Date().toString();"apps"->simApps.joinToString(" • "){it.name};"about"->"AETHRA Golden Gate Simulator — visual terminal only."; "clear"->"";""->output;else->"Command not found. Type help."}
   onOutput(if(cmd=="clear")"" else output+"\n$ "+input+"\n"+result);onInput("")
  })
 }
 Text("Visual terminal only. It does not execute Android shell commands.",color=SM,fontSize=10.sp)
}
@Composable
private fun StoreApp() {
 Text("App Store",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text("Discover the built-in simulator apps.",color=SM,fontSize=11.sp);Spacer(Modifier.height(12.dp))
 simApps.forEach { app->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(app.tint.copy(alpha=.35f)),contentAlignment=Alignment.Center){Icon(app.icon,null,tint=Color.White)}
  Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(app.name,color=SW,fontSize=12.sp);Text("Included with AETHRA",color=SM,fontSize=9.sp)}
  Text("INCLUDED",color=SB,fontSize=8.sp)
 }}
}
@Composable
private fun SettingsApp(wifi:Boolean,onWifi:(Boolean)->Unit,dark:Boolean,onDark:(Boolean)->Unit,onAndroid:()->Unit) {
 Text("System Settings",color=SW,fontSize=24.sp,fontWeight=FontWeight.Light);Text("Personalise your simulated desktop.",color=SM,fontSize=11.sp);Spacer(Modifier.height(14.dp))
 Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Wi-Fi indicator",color=SW,fontSize=13.sp);Text("Visual simulator setting only",color=SM,fontSize=9.sp)};Switch(wifi,onWifi)}
 Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Dark appearance",color=SW,fontSize=13.sp);Text("Visual simulator setting only",color=SM,fontSize=9.sp)};Switch(dark,onDark)}
 Text("Android applications",color=SB,fontSize=13.sp,modifier=Modifier.clickable(onClick=onAndroid).padding(vertical=10.dp))
 Text("These controls do not change Android system settings.",color=SM,fontSize=10.sp)
}
