package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntSize

class JetMapState(
    config: JetMapConfig,
    val canvasSize: IntSize,
    tileProvider: TileProvider
) {
    internal val motionState: MotionState = MotionState(
        canvasSize = canvasSize,
        config = config
    )

    internal val tileCanvas: TileCanvas = TileCanvas(
        tileProvider = tileProvider,
        tileSize = config.tileSize
    )

    fun drawCanvas(
        assetManager: AssetManager
    ): DrawScope.() -> Unit = {
        drawIntoCanvas { canvas ->
            tileCanvas.draw(
                canvas = canvas,
                visibleArea = motionState.visibleArea,
                assetManager = assetManager
            )
        }
    }
}