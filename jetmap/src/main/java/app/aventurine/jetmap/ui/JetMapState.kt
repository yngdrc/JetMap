package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.gesture.GestureApi
import app.aventurine.jetmap.controller.gesture.GestureController
import app.aventurine.jetmap.controller.marker.MarkerApi
import app.aventurine.jetmap.controller.marker.MarkerController
import app.aventurine.jetmap.controller.motion.MotionApi
import app.aventurine.jetmap.controller.motion.MotionController
import app.aventurine.jetmap.controller.tile.TileApi
import app.aventurine.jetmap.controller.tile.TileController
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.TileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JetMapState(
    val config: JetMapConfig,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal lateinit var motionController: MotionController

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

    val motionApi: MotionApi
        get() = motionController

    val tileApi: TileApi
        get() = tileController

    val markerApi: MarkerApi
        get() = markerController

    val gestureApi: GestureApi
        get() = gestureController

    internal fun initialize(canvasSize: IntSize) {
        motionController = MotionController(
            parentScope = scope,
            canvasSize = canvasSize,
            config = config,
        )

        collectStates(canvasSize = canvasSize)
    }

    private fun collectStates(
        canvasSize: IntSize
    ) {
        scope.launch {
            motionController.visibleAreaFlow.combine(
                flow = motionController.levelState
            ) { visibleArea, level ->
                visibleArea to level
            }.collectLatest { (visibleArea, level) ->
                tileController.onVisibleAreaChanged(visibleArea = visibleArea, level = level)
            }
        }

        scope.launch {
            motionController.motionState
                .debounce(timeout = 300.toDuration(DurationUnit.MILLISECONDS))
                .combine(
                    flow = motionController.levelState
                ) { motionState, level ->
                    motionState to level
                }.buffer(capacity = 0).collectLatest { (motionState, level) ->
                    markerController.onVisibleAreaChanged(
                        visibleAreaRect = motionState.getVisibleAreaRect(canvasSize = canvasSize),
                        level = level
                    )
                }
        }

        scope.launch {
            gestureController.tapState.collectLatest { tapState ->
                if (tapState == null) {
                    return@collectLatest
                }

                val markerState = markerController.markerState.value
                val existingMarker = markerState.firstOrNull { marker ->
                    val xOffset = marker.bitmap.width / 2
                    val yOffset = marker.bitmap.height / 2

                    marker.x.toFloat() in tapState.x - xOffset..tapState.x + xOffset
                            && marker.y.toFloat() in tapState.y - yOffset..tapState.y + yOffset
                }

                gestureController.onMarkerFocusChanged(
                    tapState = tapState,
                    existingMarker = existingMarker
                )
            }
        }
    }
}