package app.mininav

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.security.SecureRandom

object AppState {
    val defaults = linkedMapOf("background" to "#000000", "destination" to "#B8C4D6", "arrow" to "#72E8B0", "distance" to "#FFFFFF", "street" to "#FFFFFF", "duration" to "#72E8B0")
    private lateinit var prefs: android.content.SharedPreferences
    val tracker = TripTracker()
    var trip by mutableStateOf(Trip()); private set
    var theme by mutableStateOf(defaults.toMap()); private set
    var patterns by mutableStateOf(Maneuver.entries.associateWith(::defaultPattern)); private set
    var vibrationEnabled by mutableStateOf(true); private set
    var screenMode by mutableStateOf("always_on"); private set
    var source by mutableStateOf("auto"); private set
    var running by mutableStateOf(false)
    var status by mutableStateOf("Stopped")
    var lastRead by mutableStateOf(0L)
    var diagnostic by mutableStateOf("No navigation notification received")
    var manual by mutableStateOf(""); private set
    private var raw = ""
    private var sequence = 0L
    private var preview: JSONObject? = null
    private var previewUntil = 0L
    var idleSince = 0L
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("mininav", Context.MODE_PRIVATE)
        theme = defaults.mapValues { (k,v) -> prefs.getString("color.$k", v)!!.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) } ?: v }
        patterns = Maneuver.entries.associateWith { runCatching { Pattern.valueOf(prefs.getString("pattern.${it.name}", defaultPattern(it).name)!!) }.getOrDefault(defaultPattern(it)) }
        vibrationEnabled = prefs.getBoolean("vibration", true)
        // Kept in protocol v1 for older watches, but this release always emits always_on.
        screenMode = "always_on"; prefs.edit().putString("screen", screenMode).apply()
        source = prefs.getString("source", "auto")!!; tracker.sourcePreference = source
        if (!prefs.contains("token")) rotateKey()
    }
    val token get() = prefs.getString("token", "")!!
    var port: Int
        get() = prefs.getInt("port", 37843)
        set(value) { prefs.edit().putInt("port", value).apply() }
    val bridgeCode get() = "MN1:$port:$token"
    fun rotateKey() { val bytes = ByteArray(16); SecureRandom().nextBytes(bytes); prefs.edit().putString("token", bytes.joinToString("") { "%02x".format(it) }).apply() }
    @Synchronized fun color(key: String, color: String) { if(key in defaults && Regex("#[0-9a-fA-F]{6}").matches(color)) { theme = theme + (key to color); prefs.edit().putString("color.$key", color).apply(); sequence++ } }
    @Synchronized fun pattern(m: Maneuver, p: Pattern) { patterns = patterns + (m to p); prefs.edit().putString("pattern.${m.name}", p.name).apply(); sequence++ }
    @Synchronized fun vibrate(value: Boolean) { vibrationEnabled = value; prefs.edit().putBoolean("vibration", value).apply(); sequence++ }
    @Synchronized fun screen(value: String) { screenMode = "always_on"; prefs.edit().putString("screen", screenMode).apply(); sequence++ }
    @Synchronized fun source(value: String) { source = value; tracker.sourcePreference = value; prefs.edit().putString("source", value).apply(); clear() }
    @Synchronized fun manual(value: String) { manual = value.take(180); tracker.updateManual(manual); trip = tracker.trip; sequence++ }
    @Synchronized fun removed(key: String) { tracker.remove(key, System.currentTimeMillis()) }
    @Synchronized fun accept(n: Notice) {
        raw = (listOf(n.title,n.text,n.subText,n.bigText) + n.lines).joinToString("\n")
        val p = NavParser.parse(n)
        diagnostic = "Source: ${n.source}\nParsed: ${p != null}\nManeuver: ${p?.maneuver ?: "UNKNOWN"}\nField types: title=${n.title.isNotEmpty()}, text=${n.text.isNotEmpty()}, subtext=${n.subText.isNotEmpty()}, bigText=${n.bigText.isNotEmpty()}, lines=${n.lines.isNotEmpty()}\nIcon classifier: ${n.icon}"
        if (tracker.accept(n)) { trip = tracker.trip; sequence++; idleSince = if (trip.state == "arrived") System.currentTimeMillis() else 0 }
    }
    @Synchronized fun tick(now: Long) {
        if (tracker.tick(now)) { trip = tracker.trip; manual = ""; raw = ""; sequence++; idleSince = now }
        if(preview != null && now >= previewUntil) { preview = null; sequence++ }
    }
    @Synchronized fun clear() { tracker.clear(); trip = Trip(); manual = ""; raw = ""; preview = null; sequence++; idleSince = System.currentTimeMillis() }
    @Synchronized fun preview(p: Pattern) { preview = JSONObject().put("nonce", java.util.UUID.randomUUID().toString()).put("preset", p.name).put("expiresAt", System.currentTimeMillis()+10000); previewUntil = System.currentTimeMillis()+10000; sequence++ }
    @Synchronized fun json(): String = JSONObject().apply {
        put("protocolVersion", 1); put("sequence", sequence); put("servedAt", System.currentTimeMillis())
        put("sessionId",trip.sessionId); put("instructionId",trip.instructionId); put("state",trip.state)
        put("source",trip.source ?: JSONObject.NULL); put("destination",trip.destination); put("street",trip.street)
        put("turnDistanceText",trip.turnDistanceText); put("remainingText",trip.remainingText); put("maneuver",trip.maneuver.name)
        put("theme",JSONObject(theme)); put("screenMode",screenMode); put("vibrationEnabled",vibrationEnabled)
        put("vibrationMap",JSONObject(patterns.mapKeys { it.key.name }.mapValues { it.value.name }))
        put("vibrationPreview",preview ?: JSONObject.NULL)
    }.toString()
    @Synchronized fun report(includeRaw: Boolean) = "MiniNav 1.0\n$status\n$diagnostic\n" + if(includeRaw) "Notification text (may contain locations):\n$raw" else "Location text and bridge credentials excluded."
}
