package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MotionState(
    val zoom: Float,
    val rotation: Float,
    val centroid: Offset
)

fun MotionState.getVisibleArea(
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
        size = rotatedCanvasRect.size / zoom
    )
}

internal class MotionController(
    canvasSize: IntSize,
    config: JetMapConfig
) {
    val minZoom: Float
    val maxZoom: Float = config.maxZoom
    val initialCentroid: Offset

    init {
        val canvasSizeBasedZoom = canvasSize.toSize().maxDimension /
                config.mapSize.toSize().minDimension

        minZoom = canvasSizeBasedZoom.coerceIn(
            minimumValue = config.minZoom,
            maximumValue = config.maxZoom
        )

        initialCentroid = Offset(
            x = -canvasSize.width / minZoom / 2f,
            y = -canvasSize.height / minZoom / 2f
        ) + Offset(
            x = config.mapSize.width / 2f,
            y = config.mapSize.height / 2f
        )
    }

    private val _motionState: MutableStateFlow<MotionState> = MutableStateFlow(
        MotionState(
            zoom = minZoom,
            rotation = 0f,
            centroid = initialCentroid
        )
    )

    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        _motionState.update { motionState ->
            val newZoom = (motionState.zoom * zoom).coerceIn(minZoom, maxZoom)
            val newCentroid = (motionState.centroid + centroid / motionState.zoom).rotateBy(
                angle = rotation
            ) - (centroid / newZoom + pan / motionState.zoom)

            motionState.copy(
                zoom = newZoom,
                rotation = motionState.rotation + rotation,
                centroid = newCentroid
            )
        }
    }
}