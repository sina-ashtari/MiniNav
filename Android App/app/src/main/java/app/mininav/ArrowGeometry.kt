package app.mininav

/** Same 200 x 160 physical coordinates as the watch Canvas; RTL never mirrors these. */
fun arrowLines(m: Maneuver): List<List<Pair<Int, Int>>> {
    if (m in listOf(Maneuver.UNKNOWN, Maneuver.ARRIVAL, Maneuver.REROUTING)) return emptyList()
    var points = listOf(100 to 135, 100 to 30)
    var head = listOf(76 to 56, 100 to 30, 124 to 56)
    val left = m.name.endsWith("LEFT")
    if (left || m.name.endsWith("RIGHT")) {
        points = listOf(115 to 135, 115 to 60, 40 to 60)
        head = listOf(68 to 32, 40 to 60, 68 to 88)
        when {
            m.name.startsWith("SLIGHT") || m.name.startsWith("KEEP") || m.name.startsWith("EXIT") -> {
                points = listOf(125 to 135, 125 to 100, 55 to 30)
                head = listOf(55 to 68, 55 to 30, 93 to 30)
            }
            m.name.startsWith("SHARP") -> {
                points = listOf(130 to 135, 130 to 35, 50 to 115)
                head = listOf(50 to 75, 50 to 115, 90 to 115)
            }
            m.name.startsWith("UTURN") -> {
                points = listOf(140 to 135, 140 to 40, 120 to 20, 70 to 20, 50 to 40, 50 to 105)
                head = listOf(25 to 80, 50 to 105, 75 to 80)
            }
            m.name.startsWith("ROUNDABOUT") -> {
                points = listOf(120 to 145, 120 to 115, 150 to 85, 150 to 55, 120 to 25, 85 to 25, 55 to 55, 55 to 85, 20 to 85)
                head = listOf(42 to 63, 20 to 85, 42 to 107)
            }
        }
        if (!left) { points = points.map { (x,y) -> 200-x to y }; head = head.map { (x,y) -> 200-x to y } }
    }
    if (m == Maneuver.MERGE) points = listOf(55 to 135, 100 to 85, 100 to 30)
    return listOf(points, head)
}
