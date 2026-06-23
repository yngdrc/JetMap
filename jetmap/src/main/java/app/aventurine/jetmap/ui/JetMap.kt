package app.aventurine.jetmap.ui

import android.content.res.Resources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.TileProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.imageResource

@Composable
fun JetMap(
    modifier: Modifier = Modifier,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider,
    config: JetMapConfig,
    resources: Resources
) {
    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
    ) { constraints ->
        val canvasSize = IntSize(width = constraints.maxWidth, height = constraints.maxHeight)
        val jetMapState = JetMapState(
            config = config,
            canvasSize = canvasSize,
            tileProvider = tileProvider,
            markerProvider = markerProvider,
            resources = resources
        )

        val canvasPlaceables = subcompose(slotId = JetMapState::class.java.name) {
            val motionState by jetMapState.motionController.motionState.collectAsStateWithLifecycle()
            val tileState by jetMapState.tileController.tileState.collectAsStateWithLifecycle()
            val markerState by jetMapState.markerController.markerState.collectAsStateWithLifecycle()
            val focusedMarkerState by jetMapState.gestureController.focusedMarker
            val pinBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_btn_speak_now)
                .asAndroidBitmap()

            Box(modifier = Modifier.fillMaxSize()) {
                JetMapCanvas(
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                jetMapState.gestureController.onTap(
                                    offset = offset,
                                    motionState = motionState
                                )
                            }
                        )
                    },
                    onGesture = jetMapState.motionController::onGesture,
                    transformBlock = jetMapState.motionController.transformCanvas(
                        motionState = motionState
                    )
                ) {
                    drawIntoCanvas { canvas ->
                        jetMapState.tileController.draw(
                            tiles = tileState,
                            canvas = canvas
                        )
                    }

                    drawIntoCanvas { canvas ->
                        jetMapState.markerController.draw(
                            markers = markerState,
                            canvas = canvas,
                        )
                    }

                    focusedMarkerState?.let { marker ->
                        if (marker.third) return@JetMapCanvas
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawBitmap(
                                pinBitmap,
                                marker.first.x,
                                marker.first.y,
                                null
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            jetMapState.motionController.changeLevel { currentLevel ->
                                currentLevel - 1
                            }
                        }
                    ) {
                        Text(text = "+")
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            jetMapState.motionController.changeLevel { currentLevel ->
                                currentLevel + 1
                            }
                        }
                    ) {
                        Text(text = "-")
                    }
                }
            }
        }.map { measurable ->
            measurable.measure(constraints = constraints)
        }

        layout(width = canvasSize.width, height = canvasSize.height) {
            canvasPlaceables.forEach { placeable ->
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