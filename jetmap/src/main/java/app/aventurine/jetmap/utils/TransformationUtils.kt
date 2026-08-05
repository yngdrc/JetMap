package app.aventurine.jetmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import app.aventurine.jetmap.controller.motion.MotionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

fun Rect.rotateBy(angle: Float): Rect {
    val matrix = Matrix()
    matrix.rotateZ(angle)
    return matrix.map(this)
}

/**
 * Single source of truth for the map <-> screen transformation.
 *
 * The canvas transform used while drawing is
 * `translate(-centroid * zoom) -> scale(zoom) -> rotate(rotation)`,
 * which is equivalent to `screen = (rotate(map, rotation) - centroid) * zoom`.
 */
fun MotionState.mapToScreen(mapOffset: Offset): Offset =
    (mapOffset.rotateBy(angle = rotation) - centroid) * zoom

/**
 * Inverse of [mapToScreen]. Used for hit testing so taps always match what is rendered.
 */
fun MotionState.screenToMap(screenOffset: Offset): Offset =
    (screenOffset / zoom + centroid).rotateBy(angle = -rotation)
