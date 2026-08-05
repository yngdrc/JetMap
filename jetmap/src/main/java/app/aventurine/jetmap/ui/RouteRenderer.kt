package app.aventurine.jetmap.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RoutePoint
import kotlin.math.atan2

/**
 * Google Maps style route styling: a dark casing, a bright core line, a grey "already travelled"
 * overlay and dimmed dashed lines for the parts of the route that live on other floors.
 */
@Immutable
data class RouteStyle(
    val casingColor: Color = Color(0xFF1156B3),
    val routeColor: Color = Color(0xFF4285F4),
    val traveledColor: Color = Color(0xFF9AA0A6),
    val otherLevelColor: Color = Color(0xFF4285F4).copy(alpha = 0.30f),
    val arrowColor: Color = Color.White,
    val originColor: Color = Color(0xFF4285F4),
    val destinationColor: Color = Color(0xFFEA4335),
    /** Screen space width in pixels; divided by the zoom so the line keeps a constant thickness. */
    val widthPx: Float = 14f,
    val casingRatio: Float = 1.45f,
    val arrowSpacingPx: Float = 90f,
    val endpointRadiusPx: Float = 11f
)

/**
 * Pre-built [Path]s for one route/level pair. Building the path once per state change instead of
 * once per frame is the single biggest win in the render loop.
 */
@Immutable
class RoutePaths internal constructor(
    internal val activePath: Path,
    internal val otherLevelPath: Path,
    internal val traveledPath: Path,
    internal val arrowAnchors: List<Pair<Offset, Float>>
)

internal fun buildRoutePaths(
    route: Route,
    level: Int,
    traveledIndex: Int,
    arrowSpacing: Float
): RoutePaths {
    val activePath = Path()
    val otherLevelPath = Path()
    val traveledPath = Path()
    val arrowAnchors = mutableListOf<Pair<Offset, Float>>()

    route.runsAt(level = level).forEach { run ->
        activePath.appendPolyline(points = run)
        appendArrows(
            points = run,
            spacing = arrowSpacing,
            output = arrowAnchors
        )
    }

    // Everything that is not on the active floor, drawn dimmed and dashed.
    var otherRun = mutableListOf<RoutePoint>()
    route.points.forEach { point ->
        if (point.level != level) {
            otherRun.add(point)
        } else if (otherRun.isNotEmpty()) {
            otherLevelPath.appendPolyline(points = otherRun)
            otherRun = mutableListOf()
        }
    }
    if (otherRun.isNotEmpty()) {
        otherLevelPath.appendPolyline(points = otherRun)
    }

    if (traveledIndex > 0) {
        val traveled = route.points
            .take(n = (traveledIndex + 1).coerceAtMost(route.points.size))
            .filter { it.level == level }
        traveledPath.appendPolyline(points = traveled)
    }

    return RoutePaths(
        activePath = activePath,
        otherLevelPath = otherLevelPath,
        traveledPath = traveledPath,
        arrowAnchors = arrowAnchors
    )
}

private fun Path.appendPolyline(points: List<RoutePoint>) {
    if (points.size < 2) {
        return
    }

    // A single moveTo followed by lineTo keeps it one polyline, so joins and caps render properly.
    moveTo(points.first().x, points.first().y)
    for (index in 1..points.lastIndex) {
        lineTo(points[index].x, points[index].y)
    }
}

private fun appendArrows(
    points: List<RoutePoint>,
    spacing: Float,
    output: MutableList<Pair<Offset, Float>>
) {
    if (points.size < 2 || spacing <= 0f) {
        return
    }

    var accumulated = 0f
    for (index in 1..points.lastIndex) {
        val start = points[index - 1]
        val end = points[index]
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.hypot(dx, dy)
        if (length <= 0f) continue

        var offsetOnSegment = spacing - accumulated
        while (offsetOnSegment < length) {
            val t = offsetOnSegment / length
            output.add(
                Offset(x = start.x + dx * t, y = start.y + dy * t) to
                        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            )
            offsetOnSegment += spacing
        }

        accumulated = (accumulated + length) % spacing
    }
}

/**
 * Draws the route. Must be called inside the map transform; [zoom] is used to keep the stroke
 * width constant on screen.
 */
internal fun DrawScope.drawRoute(
    paths: RoutePaths,
    style: RouteStyle,
    zoom: Float
) {
    val safeZoom = zoom.coerceAtLeast(0.0001f)
    val width = style.widthPx / safeZoom
    val casingWidth = width * style.casingRatio

    if (!paths.otherLevelPath.isEmpty) {
        drawPath(
            path = paths.otherLevelPath,
            color = style.otherLevelColor,
            style = Stroke(
                width = width,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(width * 1.5f, width)
                )
            )
        )
    }

    if (paths.activePath.isEmpty) {
        return
    }

    drawPath(
        path = paths.activePath,
        color = style.casingColor,
        style = Stroke(width = casingWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    drawPath(
        path = paths.activePath,
        color = style.routeColor,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    if (!paths.traveledPath.isEmpty) {
        drawPath(
            path = paths.traveledPath,
            color = style.traveledColor,
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    val arrowSize = width * 0.35f
    paths.arrowAnchors.forEach { (position, angle) ->
        rotate(degrees = angle, pivot = position) {
            val arrow = Path().apply {
                moveTo(position.x - arrowSize, position.y - arrowSize)
                lineTo(position.x + arrowSize, position.y)
                lineTo(position.x - arrowSize, position.y + arrowSize)
            }

            drawPath(
                path = arrow,
                color = style.arrowColor,
                style = Stroke(
                    width = arrowSize * 0.6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Origin / destination pins. Drawn outside the map transform so they keep a constant screen size,
 * exactly like the A/B markers in Google Maps.
 */
internal fun DrawScope.drawRouteEndpoints(
    originScreen: Offset?,
    destinationScreen: Offset?,
    style: RouteStyle
) {
    originScreen?.let { position ->
        drawCircle(color = Color.White, radius = style.endpointRadiusPx, center = position)
        drawCircle(
            color = style.originColor,
            radius = style.endpointRadiusPx * 0.65f,
            center = position
        )
    }

    destinationScreen?.let { position ->
        drawCircle(color = Color.White, radius = style.endpointRadiusPx * 1.15f, center = position)
        drawCircle(
            color = style.destinationColor,
            radius = style.endpointRadiusPx * 0.8f,
            center = position
        )
    }
}

/**
 * The blue "you are here" chevron used while navigating.
 */
internal fun DrawScope.drawUserPosition(
    positionScreen: Offset,
    bearingDegrees: Float,
    color: Color = Color(0xFF4285F4),
    radiusPx: Float = 16f
) {
    drawCircle(color = color.copy(alpha = 0.20f), radius = radiusPx * 1.9f, center = positionScreen)
    rotate(degrees = bearingDegrees, pivot = positionScreen) {
        val chevron = Path().apply {
            moveTo(positionScreen.x + radiusPx, positionScreen.y)
            lineTo(positionScreen.x - radiusPx * 0.8f, positionScreen.y - radiusPx * 0.8f)
            lineTo(positionScreen.x - radiusPx * 0.35f, positionScreen.y)
            lineTo(positionScreen.x - radiusPx * 0.8f, positionScreen.y + radiusPx * 0.8f)
            close()
        }

        drawPath(path = chevron, color = Color.White)
        drawPath(
            path = chevron,
            color = color,
            style = Stroke(width = radiusPx * 0.25f, join = StrokeJoin.Round)
        )
    }
}

