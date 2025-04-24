package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntSize

@Composable
fun JetMap(
    modifier: Modifier = Modifier,
    tileProvider: TileProvider,
    config: JetMapConfig,
    assetManager: AssetManager
) {
    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
    ) { constraints ->
        val canvasSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val jetMapState = JetMapState(
            config = config,
            canvasSize = canvasSize,
            tileProvider = tileProvider,
        )

        val placeables = subcompose(slotId = JetMapState::class.java.name) {
            JetMapCanvas(
                modifier = Modifier,
                onGesture = jetMapState.motionState::onGesture,
                transformBlock = jetMapState.motionState.transformCanvas(),
                drawBlock = jetMapState.drawCanvas(assetManager = assetManager)
            )
        }.map { measurable ->
            measurable.measure(constraints)
        }

        layout(width = canvasSize.width, height = canvasSize.height) {
            placeables.forEach { placeable ->
                placeable.placeRelative(0, 0)
            }
        }
    }
}

@Composable
fun JetMapCanvas(
    modifier: Modifier,
    onGesture: (Offset, Offset, Float, Float) -> Unit,
    transformBlock: DrawTransform.() -> Unit,
    drawBlock: DrawScope.() -> Unit
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.White)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures(
                    panZoomLock = false,
                    onGesture = onGesture
                )
            },
        onDraw = {
            withTransform(
                transformBlock = transformBlock,
                drawBlock = drawBlock
            )
        }
    )
}