package app.aventurine.jetmap.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

internal class MotionState(
    private val canvasSize: IntSize,
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

    var zoom: Float by mutableFloatStateOf(minZoom)
    var rotation: Float by mutableFloatStateOf(0f)
    var centroid: Offset by mutableStateOf(initialCentroid)

    val visibleArea: Rect
        get() {
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

    fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        val oldZoom = this.zoom
        this.zoom = (oldZoom * zoom).coerceIn(minZoom, maxZoom)
        this.rotation += rotation
        this.centroid = (this.centroid + centroid / oldZoom).rotateBy(
            angle = rotation
        ) - (centroid / this.zoom + pan / oldZoom)
    }

    fun transformCanvas(): DrawTransform.() -> Unit = {
        translate(
            left = -centroid.x * zoom,
            top = -centroid.y * zoom
        )

        scale(
            scale = zoom,
            pivot = Offset.Zero
        )

        rotate(
            degrees = rotation,
            pivot = Offset.Zero
        )
    }
}