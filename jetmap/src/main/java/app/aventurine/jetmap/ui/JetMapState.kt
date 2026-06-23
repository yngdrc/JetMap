package app.aventurine.jetmap.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.GestureController
import app.aventurine.jetmap.controller.MarkerController
import app.aventurine.jetmap.controller.MotionController
import app.aventurine.jetmap.controller.TileController
import app.aventurine.jetmap.controller.getVisibleAreaRect
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.TileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class JetMapState(
    val config: JetMapConfig,
    canvasSize: IntSize,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider,
    resources: Resources
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val motionController: MotionController = MotionController(
        parentScope = scope,
        canvasSize = canvasSize,
        config = config
    )

    internal val tileController: TileController = TileController(
        parentScope = scope,
        tileProvider = tileProvider,
        config = config
    )

    internal val markerController: MarkerController = MarkerController(
        parentScope = scope,
        markerProvider = markerProvider,
        config = config
    )

    internal val gestureController: GestureController = GestureController()

    init {
        scope.launch {
            motionController.visibleAreaFlow.combine(
                flow = motionController.levelState
            ) { visibleArea, level ->
                visibleArea to level
            }.collect { (visibleArea, level) ->
                tileController.onVisibleAreaChanged(visibleArea = visibleArea, level = level)
            }
        }

        scope.launch {
            motionController.motionState.combine(
                flow = motionController.levelState
            ) { motionState, level ->
                motionState to level
            }.buffer(capacity = 0).collect { (motionState, level) ->
                markerController.onVisibleAreaChanged(
                    visibleAreaRect = motionState.getVisibleAreaRect(canvasSize = canvasSize),
                    level = level
                )
            }
        }

        scope.launch {
            markerController.markerState.combine(
                flow = motionController.levelState
            ) { markerState, levelState ->
                markerState to levelState
            }.combine(
                flow = gestureController.tapState.filterNotNull()
            ) { (markerState, levelState), tapState ->
                val existingMarker = markerState.firstOrNull { marker ->
                    marker.x.toFloat() in tapState.x - 15..tapState.x + 15
                            && marker.y.toFloat() in tapState.y - 15..tapState.y + 15
                }

                tapState to existingMarker
            }.collect { (tapState, existingMarker) ->
                gestureController.onMarkerFocusChanged(
                    offset = tapState,
                    existingMarker = existingMarker
                )
            }
        }
    }
}