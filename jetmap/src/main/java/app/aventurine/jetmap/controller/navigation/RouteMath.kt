package app.aventurine.jetmap.controller.navigation

import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RoutePoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Result of projecting a position onto the route polyline.
 */
internal data class SnapResult(
    val index: Int,
    val fraction: Float,
    val position: Offset,
    val distance: Float
)

internal object RouteMath {

    /**
     * Ramer-Douglas-Peucker. Removes points that do not change the shape by more than [epsilon].
     */
    fun simplify(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3 || epsilon <= 0f) {
            return points
        }

        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifyRange(points, 0, points.lastIndex, epsilon, keep)

        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun simplifyRange(
        points: List<Offset>,
        first: Int,
        last: Int,
        epsilon: Float,
        keep: BooleanArray
    ) {
        if (last <= first + 1) {
            return
        }

        var maxDistance = 0f
        var maxIndex = first

        for (index in first + 1 until last) {
            val distance = pointToSegmentDistance(points[index], points[first], points[last])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = index
            }
        }

        if (maxDistance <= epsilon) {
            return
        }

        keep[maxIndex] = true
        simplifyRange(points, first, maxIndex, epsilon, keep)
        simplifyRange(points, maxIndex, last, epsilon, keep)
    }

    /**
     * Chaikin corner cutting. Rounds the sharp 45/90 degree corners produced by grid based A*,
     * so the rendered line looks like a real map route instead of stairs.
     *
     * Warning: every iteration moves the line by up to a quarter of the neighbouring segments, so
     * on a long, string pulled polyline it can push the route into obstacles. Prefer
     * [roundCorners], which is bounded by a fixed radius.
     */
    fun chaikin(points: List<Offset>, iterations: Int): List<Offset> {
        if (points.size < 3 || iterations <= 0) {
            return points
        }

        var current = points
        repeat(iterations) {
            val smoothed = ArrayList<Offset>(current.size * 2)
            smoothed.add(current.first())

            for (index in 0 until current.lastIndex) {
                val start = current[index]
                val end = current[index + 1]
                smoothed.add(start * 0.75f + end * 0.25f)
                smoothed.add(start * 0.25f + end * 0.75f)
            }

            smoothed.add(current.last())
            current = smoothed
        }

        return current
    }

    /**
     * Rounds every corner with a quadratic Bezier fillet of at most [radius] map units.
     *
     * The curve stays inside the triangle (cut point, corner, cut point), so the route never
     * deviates from the walkable polyline by more than [radius]. The cut is additionally capped to
     * a third of the shorter neighbouring segment, which keeps short zig-zags intact.
     */
    fun roundCorners(
        points: List<Offset>,
        radius: Float,
        samplesPerCorner: Int = CORNER_SAMPLES
    ): List<Offset> {
        if (points.size < 3 || radius <= 0f || samplesPerCorner < 2) {
            return points
        }

        val result = ArrayList<Offset>(points.size + points.size * samplesPerCorner)
        result.add(points.first())

        for (index in 1 until points.lastIndex) {
            val previous = points[index - 1]
            val corner = points[index]
            val next = points[index + 1]

            val inboundLength = distance(previous, corner)
            val outboundLength = distance(corner, next)

            if (inboundLength <= MIN_SEGMENT || outboundLength <= MIN_SEGMENT) {
                result.add(corner)
                continue
            }

            val cut = min(radius, min(inboundLength, outboundLength) * MAX_CORNER_RATIO)
            val start = corner + (previous - corner) * (cut / inboundLength)
            val end = corner + (next - corner) * (cut / outboundLength)

            result.add(start)
            for (sample in 1 until samplesPerCorner) {
                result.add(
                    quadraticBezier(
                        start = start,
                        control = corner,
                        end = end,
                        t = sample.toFloat() / samplesPerCorner
                    )
                )
            }
            result.add(end)
        }

        result.add(points.last())
        return result
    }

    private fun quadraticBezier(start: Offset, control: Offset, end: Offset, t: Float): Offset {
        val inverse = 1f - t
        return start * (inverse * inverse) + control * (2f * inverse * t) + end * (t * t)
    }

    /** Bearing in degrees, 0 = +X axis, growing clockwise on screen (Y down). */
    fun bearing(from: Offset, to: Offset): Float =
        Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())).toFloat()

    /** Signed turn angle in degrees between two consecutive segments. Positive = right turn. */
    fun turnAngle(previous: Offset, pivot: Offset, next: Offset): Float {
        val inbound = pivot - previous
        val outbound = next - pivot
        val cross = inbound.x * outbound.y - inbound.y * outbound.x
        val dot = inbound.x * outbound.x + inbound.y * outbound.y
        return Math.toDegrees(atan2(cross.toDouble(), dot.toDouble())).toFloat()
    }

    fun distance(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)

    fun pointToSegmentDistance(point: Offset, start: Offset, end: Offset): Float {
        val projected = projectOnSegment(point = point, start = start, end = end).first
        return distance(point, projected)
    }

    private fun projectOnSegment(point: Offset, start: Offset, end: Offset): Pair<Offset, Float> {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy

        if (lengthSquared <= 0f) {
            return start to 0f
        }

        val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
            .coerceIn(0f, 1f)

        return Offset(x = start.x + dx * t, y = start.y + dy * t) to t
    }

    /**
     * Projects [position] onto the route. Only segments inside a window around [aroundIndex] are
     * checked, which keeps the update O(window) instead of O(route) on every position update.
     */
    fun snapToRoute(
        route: Route,
        position: Offset,
        level: Int,
        aroundIndex: Int,
        window: Int = DEFAULT_SNAP_WINDOW
    ): SnapResult? {
        val points = route.points
        if (points.size < 2) {
            return null
        }

        val from = max(0, aroundIndex - window)
        val to = min(points.lastIndex - 1, aroundIndex + window)
        if (from > to) {
            return null
        }

        var best: SnapResult? = null

        for (index in from..to) {
            val start = points[index]
            val end = points[index + 1]

            // Never snap onto a different floor.
            if (start.level != level && end.level != level) {
                continue
            }

            val (projected, fraction) = projectOnSegment(
                point = position,
                start = start.offset,
                end = end.offset
            )

            val distance = distance(position, projected)
            if (best == null || distance < best.distance) {
                best = SnapResult(
                    index = index,
                    fraction = fraction,
                    position = projected,
                    distance = distance
                )
            }
        }

        return best
    }

    /** Full scan fallback, used when the user goes far off route and re-acquires the polyline. */
    fun snapToRouteFully(route: Route, position: Offset, level: Int): SnapResult? = snapToRoute(
        route = route,
        position = position,
        level = level,
        aroundIndex = route.points.size / 2,
        window = route.points.size
    )

    fun isSignificantTurn(angle: Float): Boolean = abs(angle) >= SLIGHT_TURN_DEGREES

    const val SLIGHT_TURN_DEGREES = 20f
    const val TURN_DEGREES = 55f
    const val SHARP_TURN_DEGREES = 110f
    const val U_TURN_DEGREES = 160f
    private const val DEFAULT_SNAP_WINDOW = 24
    private const val CORNER_SAMPLES = 4
    private const val MAX_CORNER_RATIO = 0.33f
    private const val MIN_SEGMENT = 0.01f
}

