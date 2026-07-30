package app.aventurine.jetmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.ui.JetMapConfig

internal fun calculateInitialZoom(
    canvasSize: IntSize,
    config: JetMapConfig
): Float {
    val canvasSizeBasedZoom = canvasSize.toSize().maxDimension /
            config.mapSize.toSize().minDimension

    return canvasSizeBasedZoom.coerceIn(
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
    tileSize: Int
): VisibleArea {
    val visibleAreaRect = getVisibleAreaRect(canvasSize = canvasSize)
    val startX = visibleAreaRect.left.toInt() / tileSize
    val endX = visibleAreaRect.right.toInt() / tileSize
    val startY = visibleAreaRect.top.toInt() / tileSize
    val endY = visibleAreaRect.bottom.toInt() / tileSize

    return VisibleArea(
        left = startX,
        top = startY,
        right = endX,
        bottom = endY
    )
}