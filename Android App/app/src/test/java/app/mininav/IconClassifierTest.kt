package app.mininav

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class IconClassifierTest {
    private fun arrow(right:Boolean=false):IntArray {
        val lines=listOf(listOf(75 to 90,75 to 30,10 to 30),listOf(35 to 5,10 to 30,35 to 55))
        return IntArray(128*128) { i ->
            val x=if(right) 127-i%128 else i%128;val y=i/128
            if(lines.any { line -> line.zipWithNext().any { (a,b) ->
                val dx=(b.first-a.first).toDouble();val dy=(b.second-a.second).toDouble()
                val u=(((x-14-a.first)*dx+(y-14-a.second)*dy)/(dx*dx+dy*dy)).coerceIn(0.0,1.0)
                hypot(x-14-a.first-u*dx,y-14-a.second-u*dy)<=6
            } }) -1 else 0
        }
    }
    @Test fun syntheticLeftAndRight() {
        assertEquals(Maneuver.LEFT,IconClassifier.classify(arrow(),128))
        assertEquals(Maneuver.RIGHT,IconClassifier.classify(arrow(true),128))
    }
    @Test fun rejectsBlankFilledAndNonArrow() {
        assertEquals(Maneuver.UNKNOWN,IconClassifier.classify(IntArray(1024),32))
        assertEquals(Maneuver.UNKNOWN,IconClassifier.classify(IntArray(1024){-1},32))
        val circle=IntArray(1024) { if(hypot((it%32-16).toDouble(),(it/32-16).toDouble())<10) -1 else 0 }
        assertEquals(Maneuver.UNKNOWN,IconClassifier.classify(circle,32))
        assertEquals(Maneuver.UNKNOWN,IconClassifier.classify(IntArray(9),4))
    }
}
