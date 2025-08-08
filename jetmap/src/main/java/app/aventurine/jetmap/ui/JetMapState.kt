package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JetMapState(
    coroutineScope: CoroutineScope,
    config: JetMapConfig,
    canvasSize: IntSize,
    tileProvider: TileProvider,
    assetManager: AssetManager
) {
    internal val motionController: MotionController = MotionController(
        parentScope = coroutineScope,
        canvasSize = canvasSize,
        config = config
    )

    internal val tileController: TileController = TileController(
        parentScope = coroutineScope,
        tileProvider = tileProvider,
        config = config,
        assetManager = assetManager
    )

    init {
        coroutineScope.launch {
            motionController.visibleAreaFlow.collect(tileController::onVisibleAreaChanged)
        }
    }
}