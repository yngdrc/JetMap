package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class JetMapState(
    val config: JetMapConfig,
    val tileProvider: TileProvider,
    val canvasSize: Size
) {
    val minZoom: Float = max(
        a = canvasSize.width / config.mapSize.width,
        b = canvasSize.height / config.mapSize.height
    ).coerceIn(
        minimumValue = config.minZoom,
        maximumValue = config.maxZoom
    )

    var motionState by mutableStateOf(
        MotionState(
            zoom = minZoom,
            centroid = Offset(
                x = -canvasSize.width / minZoom / 2f,
                y = -canvasSize.height / minZoom / 2f
            ) + Offset(
                x = config.mapSize.width / 2f,
                y = config.mapSize.height / 2f
            )
        )
    )

    val visibleArea: Rect
        get() = Rect(
            topLeft = motionState.centroid.rotateBy(-motionState.rotation),
            bottomRight = motionState.centroid.rotateBy(-motionState.rotation) + Offset(
                x = canvasSize.width,
                y = canvasSize.height
            ) / motionState.zoom
        )

    fun transformCanvas(): DrawTransform.() -> Unit = {
        translate(
            left = -motionState.centroid.x * motionState.zoom,
            top = -motionState.centroid.y * motionState.zoom
        )

        scale(
            scale = motionState.zoom,
            pivot = Offset.Zero
        )

        rotate(
            degrees = motionState.rotation,
            pivot = Offset.Zero
        )
    }

    fun drawCanvas(
        assetManager: AssetManager
    ): DrawScope.() -> Unit = {
        val startX = (visibleArea.left / config.tileSize.width).toInt()
        val endX = (visibleArea.right / config.tileSize.width).toInt()
        val startY = (visibleArea.top / config.tileSize.height).toInt()
        val endY = (visibleArea.bottom / config.tileSize.height).toInt()

        for (x in startX..endX)
            for (y in startY..endY)
                drawIntoCanvas { canvas ->
                    tileProvider.getTileBitmap(
                        x = x * config.tileSize.width.toInt(),
                        y = y * config.tileSize.height.toInt(),
                        assetManager = assetManager
                    )?.let { tileBitmap ->
                        canvas.nativeCanvas.drawBitmap(
                            tileBitmap,
                            x * config.tileSize.width,
                            y * config.tileSize.height,
                            null
                        )
                    }
                }
    }

    fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        val newZoom = (motionState.zoom * zoom).coerceIn(
            minZoom,
            config.maxZoom
        )

        val newCentroid = (motionState.centroid + centroid / motionState.zoom).rotateBy(
            angle = rotation
        ) - (centroid / newZoom + pan / motionState.zoom)

        motionState = motionState.copy(
            zoom = newZoom,
            rotation = motionState.rotation + rotation,
            centroid = newCentroid
        )
    }

    fun Offset.rotateBy(angle: Float): Offset {
        val angleInRadians = angle * PI / 180
        return Offset(
            (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
            (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
        )
    }
}