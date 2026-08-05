package app.aventurine.jetmap.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlin.math.PI
import kotlin.math.abs

/**
 * Pan / zoom / rotate with velocity tracking, plus tap and double tap.
 *
 * Compose's `detectTransformGestures` does not report a velocity, which is why the map had no
 * inertia. This is the same algorithm plus a [VelocityTracker] fed by single finger drags.
 */
internal fun Modifier.mapGestures(
    onGestureStart: () -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onFling: (Velocity) -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectMapTransformGestures(
            onGestureStart = onGestureStart,
            onGesture = onGesture,
            onFling = onFling
        )
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = onTap,
            onDoubleTap = onDoubleTap
        )
    }

private suspend fun PointerInputScope.detectMapTransformGestures(
    onGestureStart: () -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onFling: (Velocity) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()

        awaitFirstDown(requireUnconsumed = false)
        onGestureStart()

        var canceled: Boolean
        do {
            val event = awaitPointerEvent()
            canceled = event.changes.any { it.isConsumed }

            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (rotationChange != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange, rotationChange)
                    }

                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.size == 1) {
                        val change = pressedChanges.first()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    } else {
                        // Multi touch: a pinch must not turn into a fling.
                        velocityTracker.resetTracking()
                    }

                    event.changes.forEach { change ->
                        if (change.positionChanged()) {
                            change.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (pastTouchSlop && !canceled) {
            onFling(velocityTracker.calculateVelocity())
        }
    }
}

