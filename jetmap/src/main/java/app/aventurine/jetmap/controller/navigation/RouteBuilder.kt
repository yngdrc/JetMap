package app.aventurine.jetmap.controller.navigation

import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.navigation.models.Maneuver
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RoutePoint
import app.aventurine.jetmap.controller.navigation.models.RouteStep
import app.aventurine.jetmap.provider.PathSegment
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns raw grid paths into a smooth, instruction annotated [Route].
 *
 * Pipeline per segment: Douglas-Peucker simplification -> bounded corner rounding.
 * Line-of-sight "string pulling" is done by the [app.aventurine.jetmap.provider.PathProvider],
 * because only it knows which cells are walkable. Smoothing here is deliberately local: a global
 * Chaikin pass moved the line by a quarter of every segment and pushed it through walls.
 */
internal object RouteBuilder {

    fun build(
        segments: List<PathSegment>,
        speedUnitsPerSecond: Float,
        simplifyEpsilon: Float = 0.5f,
        cornerRadius: Float = 2.5f
    ): Route? {
        val polyline = mutableListOf<RoutePoint>()

        segments.forEach { segment ->
            val raw = segment.points.map { Offset(it.x.toFloat(), it.y.toFloat()) }
            if (raw.size < 2) {
                raw.forEach { point ->
                    polyline.addIfNotDuplicate(
                        RoutePoint(x = point.x, y = point.y, level = segment.level)
                    )
                }
                return@forEach
            }

            val smoothed = RouteMath.roundCorners(
                points = RouteMath.simplify(points = raw, epsilon = simplifyEpsilon),
                radius = cornerRadius
            )

            smoothed.forEach { point ->
                polyline.addIfNotDuplicate(
                    RoutePoint(x = point.x, y = point.y, level = segment.level)
                )
            }
        }

        if (polyline.size < 2) {
            return null
        }

        val cumulative = buildCumulativeDistances(polyline)
        val steps = buildSteps(points = polyline, cumulative = cumulative)

        return Route(
            points = polyline,
            cumulativeDistances = cumulative,
            steps = steps,
            totalDistance = cumulative.last(),
            speedUnitsPerSecond = speedUnitsPerSecond
        )
    }

    private fun MutableList<RoutePoint>.addIfNotDuplicate(point: RoutePoint) {
        val last = lastOrNull()
        if (last != null && last.level == point.level &&
            abs(last.x - point.x) < EPSILON && abs(last.y - point.y) < EPSILON
        ) {
            return
        }

        add(point)
    }

    private fun buildCumulativeDistances(points: List<RoutePoint>): List<Float> {
        val distances = ArrayList<Float>(points.size)
        var total = 0f
        distances.add(0f)

        for (index in 1..points.lastIndex) {
            val previous = points[index - 1]
            val current = points[index]
            // A level change has no horizontal length; charge a fixed cost instead.
            total += if (previous.level != current.level) {
                LEVEL_CHANGE_COST
            } else {
                RouteMath.distance(previous.offset, current.offset)
            }
            distances.add(total)
        }

        return distances
    }

    private fun buildSteps(points: List<RoutePoint>, cumulative: List<Float>): List<RouteStep> {
        val steps = mutableListOf<RouteStep>()
        var stepStart = 0

        fun closeStep(endIndex: Int, maneuver: Maneuver, instruction: String) {
            if (endIndex <= stepStart) {
                return
            }

            steps.add(
                RouteStep(
                    maneuver = maneuver,
                    instruction = instruction,
                    level = points[stepStart].level,
                    startIndex = stepStart,
                    endIndex = endIndex,
                    distance = cumulative[endIndex] - cumulative[stepStart]
                )
            )
            stepStart = endIndex
        }

        var pendingManeuver = Maneuver.START
        var pendingInstruction = "Rozpocznij trasę"

        for (index in 1 until points.lastIndex) {
            val previous = points[index - 1]
            val current = points[index]
            val next = points[index + 1]

            if (current.level != next.level) {
                closeStep(index, pendingManeuver, pendingInstruction)
                pendingManeuver = if (next.level > current.level) {
                    Maneuver.LEVEL_UP
                } else {
                    Maneuver.LEVEL_DOWN
                }
                pendingInstruction = if (next.level > current.level) {
                    "Wejdź na poziom ${next.level}"
                } else {
                    "Zejdź na poziom ${next.level}"
                }
                continue
            }

            if (previous.level != current.level) {
                continue
            }

            val angle = RouteMath.turnAngle(previous.offset, current.offset, next.offset)
            if (!RouteMath.isSignificantTurn(angle)) {
                continue
            }

            val maneuver = maneuverFor(angle)
            closeStep(index, pendingManeuver, pendingInstruction)
            pendingManeuver = maneuver
            pendingInstruction = instructionFor(maneuver)
        }

        closeStep(points.lastIndex, pendingManeuver, pendingInstruction)

        steps.add(
            RouteStep(
                maneuver = Maneuver.ARRIVE,
                instruction = "Cel podróży",
                level = points.last().level,
                startIndex = points.lastIndex,
                endIndex = points.lastIndex,
                distance = 0f
            )
        )

        return steps
    }

    private fun maneuverFor(angle: Float): Maneuver {
        val magnitude = abs(angle)
        val right = angle > 0f
        return when {
            magnitude >= RouteMath.U_TURN_DEGREES -> Maneuver.U_TURN
            magnitude >= RouteMath.SHARP_TURN_DEGREES ->
                if (right) Maneuver.SHARP_RIGHT else Maneuver.SHARP_LEFT

            magnitude >= RouteMath.TURN_DEGREES ->
                if (right) Maneuver.TURN_RIGHT else Maneuver.TURN_LEFT

            else -> if (right) Maneuver.SLIGHT_RIGHT else Maneuver.SLIGHT_LEFT
        }
    }

    private fun instructionFor(maneuver: Maneuver): String = when (maneuver) {
        Maneuver.START -> "Rozpocznij trasę"
        Maneuver.STRAIGHT -> "Jedź prosto"
        Maneuver.SLIGHT_LEFT -> "Lekko w lewo"
        Maneuver.SLIGHT_RIGHT -> "Lekko w prawo"
        Maneuver.TURN_LEFT -> "Skręć w lewo"
        Maneuver.TURN_RIGHT -> "Skręć w prawo"
        Maneuver.SHARP_LEFT -> "Ostro w lewo"
        Maneuver.SHARP_RIGHT -> "Ostro w prawo"
        Maneuver.U_TURN -> "Zawróć"
        Maneuver.LEVEL_UP -> "Wejdź wyżej"
        Maneuver.LEVEL_DOWN -> "Zejdź niżej"
        Maneuver.ARRIVE -> "Cel podróży"
    }

    fun formatDistance(distance: Float): String = "${distance.roundToInt()} j."

    private const val EPSILON = 0.01f
    private const val LEVEL_CHANGE_COST = 10f
}

