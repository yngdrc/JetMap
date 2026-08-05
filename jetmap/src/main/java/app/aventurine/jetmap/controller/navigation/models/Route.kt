package app.aventurine.jetmap.controller.navigation.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * A point of the route polyline. Coordinates are in map units, [level] is the floor it belongs to.
 */
data class RoutePoint(
    val x: Float,
    val y: Float,
    val level: Int
) {
    val offset: Offset get() = Offset(x = x, y = y)
}

/**
 * Turn-by-turn maneuver kinds, mirroring what a driving/walking navigation UI shows.
 */
enum class Maneuver {
    START,
    STRAIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    LEVEL_UP,
    LEVEL_DOWN,
    ARRIVE
}

/**
 * A single instruction of the route. [startIndex] / [endIndex] index into [Route.points].
 */
data class RouteStep(
    val maneuver: Maneuver,
    val instruction: String,
    val level: Int,
    val startIndex: Int,
    val endIndex: Int,
    val distance: Float
)

/**
 * A fully built, renderable and navigable route.
 *
 * [points] is a single continuous polyline across all levels; [cumulativeDistances] holds the
 * distance from the origin to each point so progress/ETA are O(1) lookups.
 */
data class Route(
    val points: List<RoutePoint>,
    val cumulativeDistances: List<Float>,
    val steps: List<RouteStep>,
    val totalDistance: Float,
    val speedUnitsPerSecond: Float
) {
    init {
        require(points.size >= 2) { "Route needs at least two points" }
        require(points.size == cumulativeDistances.size) { "Distance table must match points" }
    }

    val origin: RoutePoint get() = points.first()
    val destination: RoutePoint get() = points.last()

    val levels: Set<Int> = points.mapTo(LinkedHashSet()) { it.level }

    val estimatedDurationSeconds: Float
        get() = totalDistance / speedUnitsPerSecond.coerceAtLeast(0.0001f)

    val bounds: Rect = run {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        points.forEach { point ->
            if (point.x < left) left = point.x
            if (point.y < top) top = point.y
            if (point.x > right) right = point.x
            if (point.y > bottom) bottom = point.y
        }
        Rect(left = left, top = top, right = right, bottom = bottom)
    }

    /**
     * Splits the polyline into contiguous runs that live on [level]. Used by the renderer to draw
     * the active floor solid and the other floors dimmed.
     */
    fun runsAt(level: Int): List<List<RoutePoint>> {
        val runs = mutableListOf<List<RoutePoint>>()
        var current = mutableListOf<RoutePoint>()

        points.forEach { point ->
            if (point.level == level) {
                current.add(point)
            } else if (current.isNotEmpty()) {
                runs.add(current)
                current = mutableListOf()
            }
        }

        if (current.isNotEmpty()) {
            runs.add(current)
        }

        return runs.filter { it.size >= 2 }
    }

    fun remainingDistanceFrom(index: Int, fractionToNext: Float): Float {
        val safeIndex = index.coerceIn(0, points.lastIndex)
        val traveled = if (safeIndex >= points.lastIndex) {
            totalDistance
        } else {
            val from = cumulativeDistances[safeIndex]
            val to = cumulativeDistances[safeIndex + 1]
            from + (to - from) * fractionToNext.coerceIn(0f, 1f)
        }

        return (totalDistance - traveled).coerceAtLeast(0f)
    }
}

