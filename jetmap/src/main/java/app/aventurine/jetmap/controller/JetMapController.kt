package app.aventurine.jetmap.controller

import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.gesture.GestureApi
import app.aventurine.jetmap.controller.gesture.GestureControllerImpl
import app.aventurine.jetmap.controller.marker.MarkerApi
import app.aventurine.jetmap.controller.marker.MarkerController
import app.aventurine.jetmap.controller.motion.MotionApi
import app.aventurine.jetmap.controller.motion.MotionController
import app.aventurine.jetmap.controller.pathfinding.AStarPathFinder
import app.aventurine.jetmap.controller.pathfinding.PathfindingController
import app.aventurine.jetmap.controller.tile.TileApi
import app.aventurine.jetmap.controller.tile.TileController
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.MinimapStitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JetMapController(
    val config: JetMapConfig,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider,
    assetManager: AssetManager
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

    internal val gestureController: GestureControllerImpl = GestureControllerImpl()
    val pathfindingController: PathfindingController = PathfindingController(
        parentScope = scope,
        pathFinder = AStarPathFinder(),
        mapStitcher = MinimapStitcher(assetManager = assetManager)
    )

    private val _initializationState: MutableState<Boolean> = mutableStateOf(false)
    val initializationState: State<Boolean> = _initializationState

    val motionApi: MotionApi
        get() = motionController

    val gestureApi: GestureApi
        get() = gestureController

    fun initialize(canvasSize: IntSize) {
        _initializationState.value = false
        motionController = MotionController(
            parentScope = scope,
            canvasSize = canvasSize,
            config = config,
        )

        collectStates(canvasSize = canvasSize)
        _initializationState.value = true
    }

    private fun collectStates(
        canvasSize: IntSize
    ) {
        scope.launch {
            motionController.visibleAreaFlow.combine(
                flow = motionController.levelState
            ) { visibleArea, level ->
                visibleArea to level
            }.combine(flow = tileController.terrainState) { (visibleArea, level), terrainType ->
                Triple(visibleArea, level, terrainType)
            }.collectLatest { (visibleArea, level, terrainType) ->
                tileController.onVisibleAreaChanged(
                    visibleArea = visibleArea,
                    level = level,
                    terrainType = terrainType
                )
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

                motionController.moveTo(offset = tapState, 5f)
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

//        scope.launch(Dispatchers.Default) {
//            gestureController.tapState.collectLatest { tapState ->
//                if (tapState == null) {
//                    return@collectLatest
//                }
//
//                val markerState = markerController.markerState.value
//                val existingMarker = markerState.firstOrNull { marker ->
//                    val xOffset = marker.bitmap.width / 2
//                    val yOffset = marker.bitmap.height / 2
//
//                    marker.x.toFloat() in tapState.x - xOffset..tapState.x + xOffset
//                            && marker.y.toFloat() in tapState.y - yOffset..tapState.y + yOffset
//                }
//
//                pathfindingController.findPath(
//                    startingPoint = existingMarker?.let { marker ->
//                        IntOffset(marker.x, marker.y)
//                    } ?: IntOffset(
//                        x = tapState.x.toInt(),
//                        y = tapState.y.toInt()
//                    ),
//                    endingPoint = IntOffset(x = 601, y = 1244)
//                )
//            }
//        }
    }
}