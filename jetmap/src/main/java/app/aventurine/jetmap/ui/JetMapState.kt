package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class JetMapState(
    config: JetMapConfig,
    canvasSize: IntSize,
    tileProvider: TileProvider
) {
    internal val motionController: MotionController = MotionController(
        canvasSize = canvasSize,
        config = config
    )

    internal val tileController: TileController = TileController(
        tileProvider = tileProvider,
        tileSize = config.tileSize
    )

    fun transformCanvas(
        motionState: MotionState
    ): DrawTransform.() -> Unit = {
        translate(
            left = -motionState.centroid.x * motionState.zoom,
            top = -motionState.centroid.y * motionState.zoom
        )

        scale(scale = motionState.zoom, pivot = Offset.Zero)
        rotate(degrees = motionState.rotation, pivot = Offset.Zero)
    }
}