package app.aventurine.jetmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.ui.JetMapConfig
import kotlin.math.max

/**
 * "Cover" zoom: the smallest zoom at which the map still fills the whole canvas.
 * Using max(...) instead of an aspect-ratio guess keeps the viewport clamped to the map bounds.
 */
internal fun calculateInitialZoom(
    canvasSize: IntSize,
    config: JetMapConfig
): Float {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) {
        return config.minZoom
    }

    val mapSize = config.mapSize
    if (mapSize.width <= 0 || mapSize.height <= 0) {
        return config.minZoom
    }

    val coverZoom = max(
        canvasSize.width.toFloat() / mapSize.width.toFloat(),
        canvasSize.height.toFloat() / mapSize.height.toFloat()
    )

    return coverZoom.coerceIn(
        minimumValue = config.minZoom,
        maximumValue = config.maxZoom
    )
}

internal fun calculateInitialCentroid(
    canvasSize: IntSize,
    minZoom: Float,
    mapSize: IntSize
): Offset {
    return Offset(
        x = -canvasSize.width / minZoom / 2f,
        y = -canvasSize.height / minZoom / 2f
    ) + Offset(
        x = mapSize.width / 2f,
        y = mapSize.height / 2f
    )
}

internal fun MotionState.getVisibleAreaRect(
    canvasSize: IntSize
): Rect {
    val rotatedCanvasRect = Rect(
        offset = Offset.Zero,
        size = canvasSize.toSize() / zoom
    ).rotateBy(angle = -rotation)

    return Rect(
        offset = centroid.rotateBy(
            angle = -rotation
        ) + rotatedCanvasRect.topLeft,
        size = rotatedCanvasRect.size
    )
}

internal fun MotionState.getVisibleArea(
    canvasSize: IntSize,
    tileSize: Int,
    margin: Int = 0
): VisibleArea {
    val visibleAreaRect = getVisibleAreaRect(canvasSize = canvasSize)
    val startX = Math.floorDiv(visibleAreaRect.left.toInt(), tileSize) - margin
    val endX = Math.floorDiv(visibleAreaRect.right.toInt(), tileSize) + margin
    val startY = Math.floorDiv(visibleAreaRect.top.toInt(), tileSize) - margin
    val endY = Math.floorDiv(visibleAreaRect.bottom.toInt(), tileSize) + margin

    return VisibleArea(
        left = startX,
        top = startY,
        right = endX,
        bottom = endY
    )
}