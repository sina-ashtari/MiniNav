package app.mininav

import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    private fun notice(title: String, street: String = "Main Street", source: String = "maps") = Notice("nav",source,title,street,"24 min")
    @Test fun englishManeuvers() {
        val phrases = listOf("Continue straight", "Slight left", "Turn left", "Sharp left", "Keep left",
            "Slight right", "Turn right", "Sharp right", "Keep right", "U-turn left", "U-turn right",
            "Roundabout left", "Roundabout right", "Exit left", "Exit right", "Merge", "You have arrived", "Rerouting", "Proceed")
        Maneuver.entries.zip(phrases).forEach { (m, phrase) -> assertEquals(phrase,m,NavParser.maneuver(phrase)) }
    }
    @Test fun persianAndDigits() {
        assertEquals("123 456 ک ی",NavParser.normalize("۱۲۳ ٤٥٦ ك ي"))
        val p = NavParser.parse(Notice("n","waze","۱۲۰ متر - به راست بپیچید","خیابان نمونه","۲۴ دقیقه"))!!
        assertEquals(Maneuver.RIGHT,p.maneuver); assertEquals("120 متر",p.turnDistance)
        assertEquals("24 دقیقه",p.remaining); assertEquals("خیابان نمونه",p.street)
        val phrases = mapOf("مستقیم" to Maneuver.STRAIGHT, "کمی چپ" to Maneuver.SLIGHT_LEFT,
            "تند راست" to Maneuver.SHARP_RIGHT, "چپ بمانید" to Maneuver.KEEP_LEFT,
            "دور بزنید راست" to Maneuver.UTURN_RIGHT, "میدان چپ" to Maneuver.ROUNDABOUT_LEFT,
            "خروجی راست" to Maneuver.EXIT_RIGHT, "محاسبه مجدد" to Maneuver.REROUTING,
            "به مقصد رسیدید" to Maneuver.ARRIVAL, "ملحق شوید" to Maneuver.MERGE)
        phrases.forEach { (phrase,m) -> assertEquals(m,NavParser.maneuver(phrase)) }
    }
    @Test fun ambiguousNeverStraight() {
        listOf("Proceed", "Roundabout", "U-turn", "Bright Street", "Left Bank Rd", "Exit Plaza").forEach {
            assertEquals(Maneuver.UNKNOWN,NavParser.maneuver(it))
        }
        assertEquals(Maneuver.LEFT, NavParser.maneuver("Turn left then turn right"))
        assertEquals(Maneuver.UNKNOWN, NavParser.maneuver("Turn left right"))
        assertEquals(Maneuver.KEEP_LEFT, NavParser.maneuver("Keep left"))
        assertEquals(Maneuver.LEFT, NavParser.maneuver("Turn left"))
        assertEquals(Maneuver.SLIGHT_RIGHT, NavParser.maneuver("Slight right"))
        assertEquals(Maneuver.SHARP_RIGHT, NavParser.maneuver("Sharp right"))
        assertEquals(Maneuver.RIGHT, NavParser.maneuver("At the roundabout, turn right"))
        assertNull(NavParser.parse(Notice("n","maps","Downloading map")))
        assertEquals(Maneuver.UNKNOWN,NavParser.parse(notice("100 m"))!!.maneuver)
    }
    @Test fun iconsAndMissingFields() {
        assertEquals(Maneuver.LEFT,NavParser.parse(Notice("n","waze",icon=Maneuver.LEFT))!!.maneuver)
        assertNull(NavParser.parse(Notice("n","waze")))
        assertEquals("",NavParser.parse(notice("Turn left"))!!.turnDistance)
        assertEquals("",NavParser.parse(Notice("n","maps","Turn right"))!!.remaining)
    }
    @Test fun duplicatesAndConsecutiveLefts() {
        val tracker = TripTracker()
        assertTrue(tracker.accept(notice("In 120 m - Turn left onto First Street")))
        val first = tracker.trip.instructionId
        assertFalse(tracker.accept(notice("In 120 m - Turn left onto First Street")))
        tracker.accept(notice("In 80 m - Turn left onto First Street"))
        assertEquals(first,tracker.trip.instructionId)
        tracker.accept(notice("In 100 m - Turn left onto Second Street"))
        assertNotEquals(first,tracker.trip.instructionId)
        val second=tracker.trip.instructionId
        tracker.clear(); tracker.accept(notice("In 100 m - Turn left onto Second Street"))
        assertNotEquals(second,tracker.trip.instructionId)
    }
    @Test fun sourceLockAndRemovalGrace() {
        val tracker=TripTracker(); tracker.accept(notice("Turn left"))
        assertFalse(tracker.accept(notice("Turn right",source="waze")))
        assertEquals("maps",tracker.trip.source)
        tracker.remove("other",10); assertFalse(tracker.tick(10000))
        tracker.remove("nav",10000); assertFalse(tracker.tick(14999)); assertTrue(tracker.tick(15000))
        assertTrue(tracker.accept(notice("Turn right",source="waze")))
        tracker.clear(); tracker.sourcePreference="maps"
        assertFalse(tracker.accept(notice("Turn right",source="waze")))
    }
    @Test fun destinationFallbackAndNoHistory() {
        val t=TripTracker(); t.updateManual("Home"); t.accept(notice("Turn left"))
        assertEquals("Home",t.trip.destination); t.updateManual("Work"); assertEquals("Work",t.trip.destination)
        t.accept(notice("Turn right").copy(subText="Destination: Station"))
        t.updateManual("Ignored"); assertEquals("Station",t.trip.destination)
        t.accept(notice("Continue straight")); assertEquals("Station",t.trip.destination)
        t.clear(); assertEquals("",t.trip.destination); assertEquals("",t.manualDestination)
    }
    @Test fun defaultsComplete() {
        assertEquals(19,Maneuver.entries.size)
        assertEquals(Pattern.LONG,defaultPattern(Maneuver.LEFT))
        assertEquals(Pattern.TWO_SHORT,defaultPattern(Maneuver.RIGHT))
        assertEquals(Pattern.STRONG_REMINDER,defaultPattern(Maneuver.UTURN_LEFT))
        assertEquals(Pattern.NONE,defaultPattern(Maneuver.UNKNOWN))
        Maneuver.entries.forEach { assertTrue(defaultPattern(it) in Pattern.entries) }
    }
}
