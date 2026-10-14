package app.mininav

import org.junit.Assert.*
import org.junit.Test

class CompletenessTest {
    @Test fun expandedNotificationFieldsFromBothSources() {
        for(source in listOf("maps", "waze")) {
            val english=NavParser.parse(Notice("n",source,"Navigation",subText="12 min · 5 km",lines=listOf("In 120 m - Turn left onto Right Street")))!!
            assertEquals(Maneuver.LEFT,english.maneuver)
            assertEquals("Right Street",english.street)
            assertEquals("120 m",english.turnDistance)
            val persian=NavParser.parse(Notice("n",source,"مسیریابی",subText="۱۲ دقیقه",lines=listOf("۱۲۰ متر - به راست بپیچید"),text="خیابان نمونه"))!!
            assertEquals(Maneuver.RIGHT,persian.maneuver)
            assertEquals("120 متر",persian.turnDistance)
            assertEquals("خیابان نمونه",persian.street)
        }
    }
    @Test fun approachPrefixDisappearingDoesNotVibrateAgain() {
        val tracker=TripTracker()
        tracker.accept(Notice("n","maps","In 120 m - Turn left onto Main"))
        val id=tracker.trip.instructionId
        tracker.accept(Notice("n","maps","Turn left onto Main"))
        assertEquals(id,tracker.trip.instructionId)
    }
    @Test fun summaryDistanceIsNotTurnDistance() {
        val p=NavParser.parse(Notice("n","maps","Turn left",subText="12 min · 5 km"))!!
        assertEquals("",p.turnDistance)
    }
    @Test fun refreshedNotificationCancelsSessionRemoval() {
        val t=TripTracker(); val n=Notice("n","maps","Turn left")
        t.accept(n); val session=t.trip.sessionId
        t.remove("n",1000); t.accept(n)
        assertFalse(t.tick(7000)); assertEquals(session,t.trip.sessionId)
    }
    @Test fun everyPreviewArrowAndDefaultMapping() {
        Maneuver.entries.forEach { m ->
            assertEquals(m !in listOf(Maneuver.UNKNOWN,Maneuver.ARRIVAL,Maneuver.REROUTING),arrowLines(m).isNotEmpty())
            arrowLines(m).flatten().forEach { (x,y) -> assertTrue(x in 0..200 && y in 0..160) }
            val expected=when {
                m.name.startsWith("UTURN") || m==Maneuver.ARRIVAL -> Pattern.STRONG_REMINDER
                m.name.endsWith("LEFT") -> Pattern.LONG
                m.name.endsWith("RIGHT") -> Pattern.TWO_SHORT
                m==Maneuver.STRAIGHT -> Pattern.SHORT_LIGHT
                m==Maneuver.MERGE || m==Maneuver.REROUTING -> Pattern.SHORT_STRONG
                else -> Pattern.NONE
            }
            assertEquals(expected,defaultPattern(m))
            if(m.name.endsWith("LEFT")) {
                val right=Maneuver.valueOf(m.name.replace("LEFT","RIGHT"))
                assertEquals(arrowLines(m).map { line -> line.map { (x,y) -> 200-x to y } },arrowLines(right))
            }
        }
    }
}
