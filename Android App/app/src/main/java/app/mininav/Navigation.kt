package app.mininav

import java.security.MessageDigest
import java.util.UUID

enum class Maneuver {
    STRAIGHT, SLIGHT_LEFT, LEFT, SHARP_LEFT, KEEP_LEFT, SLIGHT_RIGHT, RIGHT, SHARP_RIGHT,
    KEEP_RIGHT, UTURN_LEFT, UTURN_RIGHT, ROUNDABOUT_LEFT, ROUNDABOUT_RIGHT, EXIT_LEFT,
    EXIT_RIGHT, MERGE, ARRIVAL, REROUTING, UNKNOWN
}
enum class Pattern { NONE, SHORT_LIGHT, SHORT_STRONG, TWO_SHORT, LONG, EXTRA_LONG, STRONG_REMINDER }
fun defaultPattern(m: Maneuver): Pattern = when {
    m.name.startsWith("UTURN") || m == Maneuver.ARRIVAL -> Pattern.STRONG_REMINDER
    m.name.endsWith("LEFT") -> Pattern.LONG
    m.name.endsWith("RIGHT") -> Pattern.TWO_SHORT
    m == Maneuver.STRAIGHT -> Pattern.SHORT_LIGHT
    m == Maneuver.MERGE || m == Maneuver.REROUTING -> Pattern.SHORT_STRONG
    else -> Pattern.NONE
}
data class Notice(val key: String, val source: String, val title: String = "", val text: String = "",
    val subText: String = "", val bigText: String = "", val lines: List<String> = emptyList(),
    val icon: Maneuver = Maneuver.UNKNOWN)
data class Instruction(val maneuver: Maneuver, val street: String, val destination: String,
    val turnDistance: String, val remaining: String, val stableText: String)
data class Trip(val sessionId: String = "", val instructionId: String = "", val state: String = "idle",
    val source: String? = null, val destination: String = "", val street: String = "",
    val turnDistanceText: String = "", val remainingText: String = "", val maneuver: Maneuver = Maneuver.UNKNOWN)

object NavParser {
    fun normalize(value: String): String = value.map { c -> when(c) {
        in '۰'..'۹' -> '0' + (c - '۰'); in '٠'..'٩' -> '0' + (c - '٠')
        'ي' -> 'ی'; 'ك' -> 'ک'; '\u200c', '\u200e', '\u200f' -> ' '; else -> c
    } }.joinToString("").lowercase().replace(Regex("\\s+"), " ").trim()
    private val distance = Regex("(?<![\\p{L}\\d])\\d+(?:[.,٫]\\d+)?\\s*(?:km|kilometers?|miles?|mi|meters?|m|ft|feet|کیلومتر|متر)(?![\\p{L}])", RegexOption.IGNORE_CASE)
    private val duration = Regex("\\d+\\s*(?:hours?|hrs?|hr|h|minutes?|mins?|min|ساعت|دقیقه)(?![\\p{L}])(?:\\s*و?\\s*\\d+\\s*(?:minutes?|mins?|min|دقیقه))?", RegexOption.IGNORE_CASE)
    fun maneuver(value: String): Maneuver {
        // Only instruction fields reach this function. Anchor them so street/destination text
        // (for example "Left Bank Rd" or "Exit Plaza") cannot become a maneuver.
        var t = normalize(value).split(Regex("\\s+(?:onto|on to|toward|towards|به سمت|به خیابان)\\s+"), limit = 2)[0]
        t = t.replace(Regex("^(?:in|after|at)?\\s*${distance.pattern}\\s*[-–—:،,•]*\\s*"), "")
        t = t.replace(Regex("^(?:at the roundabout|در میدان)\\s*[,،:-]?\\s*"), "")
        t = t.replace(Regex("^به\\s+(?=(?:چپ|راست)(?:\\s|$))"), "")
        // The next maneuver is deliberately ignored: the rider needs the immediate action.
        t = t.split(Regex("\\s+(?:then|and then|سپس|بعد)\\s+"), limit = 2)[0].trim()
        fun has(vararg terms: String) = terms.any { Regex("(?<![\\p{L}])${Regex.escape(it)}(?![\\p{L}])").containsMatchIn(t) }
        fun starts(vararg terms: String) = terms.any { Regex("^${Regex.escape(it)}(?:\\s|$)").containsMatchIn(t) }
        if (!starts("rerouting", "recalculating", "محاسبه مجدد", "مسیریابی مجدد", "you have arrived", "you've arrived", "arrived", "به مقصد رسیدید", "رسیده اید", "u-turn", "u turn", "دور بزنید", "دوربرگردان", "roundabout", "میدان", "exit", "خروجی", "keep", "stay", "بمانید", "slight", "slightly", "کمی", "ملایم", "sharp", "تند", "merge", "ادغام", "ملحق شوید", "turn", "بپیچید", "بپیچ", "گردش", "continue", "go", "straight", "مستقیم", "چپ", "راست")) return Maneuver.UNKNOWN
        val left = has("left", "چپ"); val right = has("right", "راست")
        if (left && right) return Maneuver.UNKNOWN
        return when {
            has("rerouting", "recalculating", "محاسبه مجدد", "مسیریابی مجدد") -> Maneuver.REROUTING
            has("you have arrived", "you've arrived", "arrived", "به مقصد رسیدید", "رسیده اید") -> Maneuver.ARRIVAL
            has("u-turn", "u turn", "دور بزنید", "دوربرگردان") -> when { left -> Maneuver.UTURN_LEFT; right -> Maneuver.UTURN_RIGHT; else -> Maneuver.UNKNOWN }
            has("roundabout", "میدان") -> when { left -> Maneuver.ROUNDABOUT_LEFT; right -> Maneuver.ROUNDABOUT_RIGHT; else -> Maneuver.UNKNOWN }
            has("exit", "خروجی") -> when { left -> Maneuver.EXIT_LEFT; right -> Maneuver.EXIT_RIGHT; else -> Maneuver.UNKNOWN }
            has("keep", "stay", "بمانید") -> when { left -> Maneuver.KEEP_LEFT; right -> Maneuver.KEEP_RIGHT; else -> Maneuver.UNKNOWN }
            has("slight", "slightly", "کمی", "ملایم") -> when { left -> Maneuver.SLIGHT_LEFT; right -> Maneuver.SLIGHT_RIGHT; else -> Maneuver.UNKNOWN }
            has("sharp", "تند") -> when { left -> Maneuver.SHARP_LEFT; right -> Maneuver.SHARP_RIGHT; else -> Maneuver.UNKNOWN }
            has("merge", "ادغام", "ملحق شوید") -> Maneuver.MERGE
            has("turn", "بپیچید", "بپیچ", "گردش") -> when { left -> Maneuver.LEFT; right -> Maneuver.RIGHT; else -> Maneuver.UNKNOWN }
            starts("continue", "go", "straight", "مستقیم") && has("straight", "مستقیم") -> Maneuver.STRAIGHT
            else -> Maneuver.UNKNOWN
        }
    }
    fun parse(n: Notice): Instruction? {
        val primary = (listOf(n.title, n.text, n.bigText) + n.lines).filter { it.isNotBlank() }.distinct()
        val all = (primary + n.subText).filter { it.isNotBlank() }
        val normalized = all.map(::normalize)
        val textManeuver = primary.asSequence().map(::maneuver).firstOrNull { it != Maneuver.UNKNOWN } ?: Maneuver.UNKNOWN
        val m = if (textManeuver != Maneuver.UNKNOWN) textManeuver else n.icon
        val remaining = normalized.asSequence().mapNotNull { duration.find(it)?.value }.firstOrNull().orEmpty()
        val instruction = primary.firstOrNull { maneuver(it) != Maneuver.UNKNOWN } ?: n.title
        // A distance-only title or a maneuver-bearing field is eligible, never the trip summary.
        val turnDistance = distance.find(normalize(n.title))?.value
            ?: distance.find(normalize(instruction))?.value.orEmpty()
        if (m == Maneuver.UNKNOWN && !(turnDistance.isNotEmpty() && remaining.isNotEmpty())) return null
        val onto = Regex("(?:onto|towards?|on to|به سمت|به خیابان)\\s+(.+)", RegexOption.IGNORE_CASE).find(instruction)?.groupValues?.get(1)
        val street = (onto ?: n.text.takeIf { duration.find(normalize(it)) == null && maneuver(it) == Maneuver.UNKNOWN }
            ?: "").substringBefore(" • ").trim().take(180)
        val destination = all.asSequence().mapNotNull {
            Regex("(?:destination|مقصد)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)
        }.firstOrNull().orEmpty().take(180)
        val normalizedInstruction = normalize(instruction)
        val withoutApproach = Regex("^(?:(?:in|after|در|پس از)\\s+)?${distance.pattern}\\s*[-–—:،,•]*\\s*")
            .replace(normalizedInstruction, "")
        val stable = distance.replace(duration.replace(withoutApproach, ""), "").replace(Regex("\\s+"), " ").trim()
        return Instruction(m, street, destination, turnDistance, remaining, stable)
    }
}

class TripTracker {
    var trip = Trip(); private set
    var sourcePreference = "auto"
    var manualDestination = ""
    private val activeKeys = mutableSetOf<String>()
    private var sourceDestination = ""
    var removedAt: Long? = null; private set
    fun accept(n: Notice): Boolean {
        if (sourcePreference != "auto" && n.source != sourcePreference) return false
        if (trip.source != null && trip.source != n.source) return false
        val p = NavParser.parse(n) ?: return false
        activeKeys.add(n.key); removedAt = null
        val session = trip.sessionId.ifEmpty { UUID.randomUUID().toString() }
        val identity = listOf(session, p.maneuver.name, p.stableText, NavParser.normalize(p.street)).joinToString("|")
        val id = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).take(16).joinToString("") { "%02x".format(it) }
        if (p.destination.isNotBlank()) sourceDestination = p.destination
        val next = Trip(session, id, when(p.maneuver) { Maneuver.ARRIVAL -> "arrived"; Maneuver.REROUTING -> "rerouting"; else -> "navigating" },
            n.source, sourceDestination.ifEmpty { manualDestination }, p.street, p.turnDistance, p.remaining, p.maneuver)
        val changed = next != trip; trip = next; return changed
    }
    fun remove(key: String, now: Long) { if (activeKeys.remove(key) && activeKeys.isEmpty()) removedAt = now }
    fun tick(now: Long): Boolean { if (removedAt?.let { now - it >= 5000 } == true) { clear(); return true }; return false }
    fun updateManual(value: String) {
        manualDestination = value
        if (trip.sessionId.isNotEmpty() && sourceDestination.isEmpty()) trip = trip.copy(destination = value)
    }
    fun clear() { trip = Trip(); activeKeys.clear(); removedAt = null; manualDestination = ""; sourceDestination = "" }
}
