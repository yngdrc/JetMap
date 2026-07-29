package app.aventurine.jetmap.ui

import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.path.PathState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JetMap(
    modifier: Modifier = Modifier,
    jetMapController: JetMapController,
    @DrawableRes myLocationResId: Int,
    backgroundColorSelector: (Int) -> Color = { Color.White }
) {
    SubcomposeLayout(
        modifier = modifier.fillMaxSize()
    ) { constraints ->
        val canvasSize = IntSize(width = constraints.maxWidth, height = constraints.maxHeight)
        jetMapController.initialize(canvasSize = canvasSize)

        val canvasPlaceables = subcompose(slotId = JetMapController::class.java.name) {
            val motionState by jetMapController.motionController.motionStateFlow
                .collectAsStateWithLifecycle()

            val tileState by jetMapController.tileController.tileStateFlow
                .collectAsStateWithLifecycle()

            val markerState by jetMapController.markerController.markerStateFlow
                .collectAsStateWithLifecycle()

            val pathState by jetMapController.pathController.pathStateFlow
                .collectAsStateWithLifecycle(initialValue = null)

            val level by jetMapController.motionController.levelStateFlow
                .collectAsStateWithLifecycle()

            val focusedMarker by jetMapController.gestureController.focusedMarkerFlow
                .collectAsStateWithLifecycle()

            val uiState by jetMapController.uiController.uiState
            val pinBitmap = ImageBitmap.imageResource(id = myLocationResId).asAndroidBitmap()

            JetMapCanvas(
                modifier = modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            jetMapController.gestureController.onTap(
                                tapArea = 11.toDp().toPx() / 2,
                                offset = offset,
                                motionState = motionState,
                                level = level
                            )
                        }
                    )
                },
                onGesture = jetMapController.motionController::onGesture,
                backgroundColor = backgroundColorSelector(level),
                transformBlock = jetMapController.motionController.transformCanvas(
                    motionState = motionState
                )
            ) {
                drawIntoCanvas { canvas ->
                    jetMapController.tileController.draw(
                        tiles = tileState,
                        canvas = canvas
                    )
                }

                pathState?.let { pathState ->
                    if (pathState !is PathState.RouteFound) {
                        return@let
                    }

                    drawIntoCanvas { canvas ->
                        val segments = pathState.pathData[level] ?: return@drawIntoCanvas
                        val path = Path().apply {
                            // Każdy segment rysowany osobno — piętro może mieć wiele segmentów (np. most)
                            segments.forEach { segment ->
                                segment.forEachIndexed { index, offset ->
                                    val previous = segment.getOrNull(index - 1) ?: return@forEachIndexed
                                    moveTo(previous.x.toFloat(), previous.y.toFloat())
                                    lineTo(offset.x.toFloat(), offset.y.toFloat())
                                }
                            }
                        }

                        canvas.nativeCanvas.drawPath(
                            path,
                            Paint().apply {
                                style = Paint.Style.STROKE
                                color = android.graphics.Color.WHITE
                                strokeWidth = 1f
                            }
                        )
                    }
                }

                drawIntoCanvas { canvas ->
                    jetMapController.markerController.draw(
                        markers = markerState,
                        focusedMarker = focusedMarker,
                        level = level,
                        pinBitmap = pinBitmap,
                        canvas = canvas,
                        showMarkers = uiState.showMarkers
                    )
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
    backgroundColor: Color,
    onGesture: (Offset, Offset, Float, Float) -> Unit,
    transformBlock: DrawTransform.() -> Unit,
    drawBlock: DrawScope.() -> Unit
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .clipToBounds()
            .pointerInput(key1 = Unit) {
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