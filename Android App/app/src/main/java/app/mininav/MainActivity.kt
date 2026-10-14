package app.mininav

import android.Manifest
import android.content.*
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); AppState.init(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF72E8B0))) { Surface { MiniNav() } } }
    }
}
private fun paint(s: String) = Color(android.graphics.Color.parseColor(s))
private fun label(value: String) = value.lowercase().replace('_',' ').replaceFirstChar { it.uppercase() }

@Composable private fun <T> Choice(title: String, selected: T, options: List<T>, display: (T)->String = { label(it.toString()) }, change: (T)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(title, style=MaterialTheme.typography.labelLarge)
        Box { OutlinedButton(onClick={expanded=true}) { Text(display(selected)) }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                options.forEach { option -> DropdownMenuItem(text={Text(display(option))},onClick={change(option);expanded=false}) }
            }
        }
    }
}
@Composable private fun MiniNav() {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var consent by remember { mutableStateOf(false) }
    var rawConfirm by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { delay(1000); now=System.currentTimeMillis() } }
    val connected = AppState.lastRead > 0 && now-AppState.lastRead < 10000
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text("MiniNav",fontSize=30.sp,color=MaterialTheme.colorScheme.primary)
        Text(if(connected) "Zepp phone bridge connected" else AppState.status,style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            listOf("Trip","Style","Vibration","Help").forEachIndexed { i,s -> TextButton(onClick={page=i}) { Text(s) } }
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.fillMaxSize()) {
            when(page) {
                0 -> {
                    item { WatchPreview() }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={
                            if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                                (context as ComponentActivity).requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),10)
                            context.startForegroundService(Intent(context,BridgeService::class.java))
                        },enabled=!AppState.running) { Text("Start bridge") }
                        OutlinedButton(onClick={context.stopService(Intent(context,BridgeService::class.java))},enabled=AppState.running) { Text("Stop") }
                    } }
                    item { Text("Start the bridge, then start a route in Waze or Maps and open MiniNav on your watch.") }
                    item { Choice("Navigation source",AppState.source,listOf("auto","maps","waze"),change=AppState::source) }
                    item { OutlinedTextField(value=AppState.manual,onValueChange=AppState::manual,label={Text("Destination if unavailable")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
                    item { Button(onClick={consent=true}) { Text("Enable notification access") } }
                    item { OutlinedButton(onClick={
                        val clipboard=context.getSystemService(ClipboardManager::class.java)
                        val code=AppState.bridgeCode
                        val clip = ClipData.newPlainText("MiniNav bridge code",code)
                        if(Build.VERSION.SDK_INT>=33) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true) }
                        clipboard.setPrimaryClip(clip)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if(clipboard.primaryClip?.getItemAt(0)?.text?.toString()==code) {
                                if(Build.VERSION.SDK_INT>=28) clipboard.clearPrimaryClip() else clipboard.setPrimaryClip(ClipData.newPlainText("",""))
                            }
                        },60000)
                    }) { Text("Copy Zepp connection code") }; Text("Paste once in Zepp → MiniNav → Settings. Clipboard clears after 60 seconds.",fontSize=12.sp) }
                    item { Text("Demonstration (bridge required)")
                        Row { TextButton(enabled=AppState.running,onClick={AppState.accept(Notice("demo","maps","In 120 m - Turn left onto Sample Street","Sample Street","24 min"))}) { Text("English") }
                            TextButton(enabled=AppState.running,onClick={AppState.accept(Notice("demo","maps","۱۲۰ متر - به راست بپیچید","خیابان نمونه","۲۴ دقیقه"))}) { Text("فارسی") }
                            TextButton(onClick=AppState::clear) { Text("Clear") } }
                    }
                }
                1 -> {
                    item { WatchPreview() }
                    items(AppState.defaults.keys.toList()) { key ->
                        var input by remember(AppState.theme[key]) { mutableStateOf(AppState.theme.getValue(key)) }
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(32.dp).background(paint(AppState.theme.getValue(key)),CircleShape))
                            OutlinedTextField(value=input,onValueChange={input=it;AppState.color(key,it)},label={Text(label(key))},singleLine=true)
                        }
                    }
                    item {
                        val bg = luminance(AppState.theme.getValue("background"))
                        if(AppState.theme.filterKeys { it!="background" }.values.any { val fg=luminance(it); (maxOf(bg,fg)+0.05)/(minOf(bg,fg)+0.05)<4.5 })
                            Text("Some colors have low contrast. Check readability on your watch.",color=MaterialTheme.colorScheme.error)
                        TextButton(onClick={AppState.defaults.forEach(AppState::color)}) { Text("Reset colors") }
                    }
                    item { Text("Always on uses more battery. Battery saver shows the latest card when you wake the watch.") }
                }
                2 -> {
                    item { Row(verticalAlignment=Alignment.CenterVertically) { Text("Vibration enabled",Modifier.weight(1f)); Switch(checked=AppState.vibrationEnabled,onCheckedChange=AppState::vibrate) } }
                    item { Text("One pattern per new instruction. Time and distance updates stay silent.") }
                    items(Maneuver.entries) { m ->
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Choice(label(m.name),AppState.patterns.getValue(m),Pattern.entries,change={AppState.pattern(m,it)}) }
                            TextButton(enabled=AppState.running && AppState.vibrationEnabled,onClick={AppState.preview(AppState.patterns.getValue(m))}) { Text("Test") }
                        }
                    }
                    item { TextButton(onClick={Maneuver.entries.forEach { AppState.pattern(it,defaultPattern(it)) }}) { Text("Reset vibration patterns") } }
                }
                else -> {
                    item { Text("Your navigation stays local",style=MaterialTheme.typography.titleLarge)
                        Text("MiniNav reads Waze and Google Maps notifications after you grant access. It sends current guidance only to the Zepp app on this phone and your watch. No accounts, analytics, cloud service, or navigation history. Notification access can be revoked in Android settings.") }
                    item { Text("Setup: install MiniNav on your Active 2 Round, paste the connection code into its settings in Zepp, start the bridge here, and open the watch app. Keep Zepp allowed to run in the background. If the bridge port changes, copy the new code again.") }
                    item { Text(AppState.diagnostic) }
                    item { Button(onClick={share(context,AppState.report(false))}) { Text("Share redacted diagnostics") } }
                    item { TextButton(onClick={rawConfirm=true}) { Text("Share with notification text…") } }
                    item { OutlinedButton(enabled=!AppState.running,onClick={AppState.rotateKey()}) { Text("Replace connection key") }; Text("Stop the bridge first. Paste the new code into Zepp afterward.") }
                }
            }
        }
    }
    if(consent) AlertDialog(onDismissRequest={consent=false},title={Text("Navigation notification access")},text={Text("Android grants broad notification access. MiniNav immediately filters to Waze and Maps and processes their navigation text and icons locally. Other apps’ notification content is not stored or forwarded.")},confirmButton={TextButton(onClick={consent=false;context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}) {Text("Open settings")}},dismissButton={TextButton(onClick={consent=false}) {Text("Cancel")}})
    if(rawConfirm) AlertDialog(onDismissRequest={rawConfirm=false},title={Text("Include private navigation text?")},text={Text("This report can contain street names and your destination. Review the report before sending it to anyone.")},confirmButton={TextButton(onClick={rawConfirm=false;share(context,AppState.report(true))}) {Text("Continue to share")}},dismissButton={TextButton(onClick={rawConfirm=false}) {Text("Cancel")}})
}
private fun share(context: Context,text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share MiniNav diagnostics")) }
private fun luminance(hex: String): Double {
    val n=android.graphics.Color.parseColor(hex)
    fun channel(v:Int):Double { val x=v/255.0;return if(x<=0.04045)x/12.92 else Math.pow((x+0.055)/1.055,2.4) }
    return 0.2126*channel(n shr 16 and 255)+0.7152*channel(n shr 8 and 255)+0.0722*channel(n and 255)
}
@Composable private fun WatchPreview() {
    val t=AppState.trip;val c=AppState.theme
    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
        Column(Modifier.size(280.dp).clip(CircleShape).background(paint(c.getValue("background"))).padding(horizontal=30.dp,vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(t.destination.ifEmpty { "MiniNav" },color=paint(c.getValue("destination")),maxLines=1)
            Canvas(Modifier.size(82.dp)) {
                val color=paint(c.getValue("arrow"))
                arrowLines(t.maneuver).forEach { line -> line.zipWithNext().forEach { (a,b) ->
                    drawLine(color, Offset(a.first/200f*size.width,a.second/160f*size.height),
                        Offset(b.first/200f*size.width,b.second/160f*size.height),size.width*0.075f,StrokeCap.Round)
                } }
            }
            if(arrowLines(t.maneuver).isEmpty()) Text(if(t.state=="idle") "Ready" else label(t.maneuver.name),color=paint(c.getValue("arrow")))
            Text(t.turnDistanceText,color=paint(c.getValue("distance")),fontSize=20.sp)
            Text(t.street.ifEmpty { "Waiting for navigation" },color=paint(c.getValue("street")),maxLines=2,textAlign=TextAlign.Center)
            Text(t.remainingText.ifEmpty { "—" },color=paint(c.getValue("duration")),fontSize=20.sp)
        }
    }
}
