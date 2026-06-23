package app.aventurine.jetmap.controller.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import app.aventurine.jetmap.utils.rotateBy

data class MotionState(
    val zoom: Float,
    val rotation: Float,
    val centroid: Offset
) {
    fun getVisibleAreaRect(
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

    fun getVisibleArea(
        canvasSize: IntSize,
        tileSize: Int
    ): VisibleArea {
        val visibleAreaRect = getVisibleAreaRect(canvasSize = canvasSize)
        val startX = visibleAreaRect.left.toInt() / tileSize
        val endX = visibleAreaRect.right.toInt() / tileSize
        val startY = visibleAreaRect.top.toInt() / tileSize
        val endY = visibleAreaRect.bottom.toInt() / tileSize

        return VisibleArea(
            startX..endX,
            startY..endY
        )
    }
}