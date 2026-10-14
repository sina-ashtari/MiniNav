package app.mininav

import kotlin.math.*

/** Conservative geometric templates. Unsupported/ambiguous shapes remain UNKNOWN. */
object IconClassifier {
    // ponytail: synthetic silhouettes, not a learned source-specific model. Add verified source
    // templates after capturing real Maps/Waze fixtures; never lower confidence to guess.
    private val paths = mapOf(
        Maneuver.STRAIGHT to listOf(listOf(50 to 90,50 to 10),listOf(25 to 35,50 to 10,75 to 35)),
        Maneuver.LEFT to listOf(listOf(75 to 90,75 to 30,10 to 30),listOf(35 to 5,10 to 30,35 to 55)),
        Maneuver.SLIGHT_LEFT to listOf(listOf(80 to 90,80 to 60,20 to 10),listOf(20 to 45,20 to 10,55 to 10)),
        Maneuver.SHARP_LEFT to listOf(listOf(80 to 90,80 to 10,20 to 70),listOf(20 to 35,20 to 70,55 to 70)),
        Maneuver.UTURN_LEFT to listOf(listOf(80 to 90,80 to 25,65 to 10,35 to 10,20 to 25,20 to 70),listOf(0 to 50,20 to 70,40 to 50)),
        Maneuver.MERGE to listOf(listOf(50 to 90,50 to 10),listOf(15 to 90,50 to 55),listOf(25 to 35,50 to 10,75 to 35))
    )
    private val templates by lazy {
        val variants=paths.toMutableMap()
        paths.filterKeys { it.name.endsWith("LEFT") }.forEach { (m,lines) ->
            variants[Maneuver.valueOf(m.name.replace("LEFT","RIGHT"))] = lines.map { line -> line.map { (x,y) -> 100-x to y } }
        }
        variants.mapValues { (_,lines) ->
            val mask=BooleanArray(128*128)
            for(y in 0..127) for(x in 0..127) mask[y*128+x] = lines.any { line -> line.zipWithNext().any { (a,b) ->
                val dx=(b.first-a.first).toDouble(); val dy=(b.second-a.second).toDouble()
                val u=(((x-14-a.first)*dx+(y-14-a.second)*dy)/(dx*dx+dy*dy)).coerceIn(0.0,1.0)
                hypot(x-14-a.first-u*dx,y-14-a.second-u*dy)<=6
            } }
            normalize(mask,128)!!
        }
    }
    fun classify(pixels: IntArray, width: Int): Maneuver {
        if(width < 8 || pixels.size != width*width) return Maneuver.UNKNOWN
        fun light(p:Int) = ((p shr 16 and 255)*299+(p shr 8 and 255)*587+(p and 255)*114)/1000
        val corners=listOf(pixels.first(),pixels[width-1],pixels[pixels.size-width],pixels.last())
        val transparent = corners.count { it.ushr(24)<100 } >= 3
        val background=corners.map(::light).average()
        val mask=BooleanArray(pixels.size) { pixels[it].ushr(24)>100 && (transparent || abs(light(pixels[it])-background)>80) }
        val normalized=normalize(mask,width) ?: return Maneuver.UNKNOWN
        val ranked=templates.map { (m,template) ->
            var intersection=0; var union=0
            normalized.indices.forEach { i -> if(normalized[i] && template[i]) intersection++; if(normalized[i] || template[i]) union++ }
            m to intersection.toDouble()/union.coerceAtLeast(1)
        }.sortedByDescending { it.second }
        return if(ranked[0].second>=0.68 && ranked[0].second-ranked[1].second>=0.08) ranked[0].first else Maneuver.UNKNOWN
    }
    private fun normalize(mask:BooleanArray,width:Int):BooleanArray? {
        val points=mask.indices.filter { mask[it] }
        if(points.size !in (mask.size/30)..(mask.size*3/4)) return null
        val x0=points.minOf { it%width }; val x1=points.maxOf { it%width }
        val y0=points.minOf { it/width }; val y1=points.maxOf { it/width }
        if(x1-x0<3 || y1-y0<width/3) return null
        return BooleanArray(32*32) { i ->
            val x=x0+(i%32)*(x1-x0)/31; val y=y0+(i/32)*(y1-y0)/31
            mask[y*width+x]
        }
    }
}
