package app.aventurine.jetmap.controller

import android.content.res.AssetManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.gesture.GestureController
import app.aventurine.jetmap.controller.marker.MarkerController
import app.aventurine.jetmap.controller.motion.MotionController
import app.aventurine.jetmap.controller.pathfinding.AStarPathFinder
import app.aventurine.jetmap.controller.pathfinding.PathfindingController
import app.aventurine.jetmap.controller.tile.TileController
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.MinimapStitcher
import app.aventurine.jetmap.utils.getVisibleAreaRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class JetMapController(
    val config: JetMapConfig,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider,
    assetManager: AssetManager
) {
    private val scope = CoroutineScope(context = SupervisorJob() + Dispatchers.Main)

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

    internal val gestureController: GestureController = GestureController(
        parentScope = scope,
        markerProvider = markerProvider
    )
    val pathfindingController: PathfindingController = PathfindingController(
        parentScope = scope,
        pathFinder = AStarPathFinder(),
        mapStitcher = MinimapStitcher(assetManager = assetManager)
    )

    private val _initializationState: MutableState<Boolean> = mutableStateOf(false)
    val initializationState: State<Boolean> = _initializationState

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
            combine(
                flow = motionController.visibleAreaFlow,
                flow2 = motionController.levelStateFlow,
                flow3 = tileController.terrainTypeStateFlow
            ) { visibleArea, level, terrainType ->
                Triple(visibleArea, level, terrainType)
            }.collectLatest { (visibleArea, level, terrainType) ->
                tileController.onVisibleAreaChanged(
                    visibleArea = visibleArea,
                    level = level,
                    terrainType = terrainType
                )
            }
        }

//        scope.launch {
//            combine(
//                flow = motionController.motionStateFlow.debounce(timeout = 300.milliseconds),
//                flow2 = motionController.levelStateFlow
//            ) { motionState, level ->
//                motionState to level
//            }.buffer(capacity = 0)
//                .collectLatest { (motionState, level) ->
//                    markerController.onVisibleAreaChanged(
//                        visibleAreaRect = motionState.getVisibleAreaRect(canvasSize = canvasSize),
//                        level = level
//                    )
//                }
//        }

        scope.launch {
            gestureController.tapFlow
                .filterNotNull()
                .collectLatest { (offset, level) ->
                    gestureController.onMarkerFocusChanged(
                        offset = offset,
                        level = level
                    )
                }
        }

//        scope.launch {
//            gestureController.focusedMarkerFlow
//                .filterNotNull()
//                .collectLatest { markerDescriptor ->
//                    motionController.moveTo(
//                        offset = Offset(
//                            x = markerDescriptor.x.toFloat(),
//                            y = markerDescriptor.y.toFloat()
//                        ),
//                        level = markerDescriptor.z,
//                        zoom = 5f
//                    )
//                }
//        }

        scope.launch {
            gestureController.focusedMarkerFlow
                .collectLatest { markerDescriptor ->
                    if (markerDescriptor == null) {
                        return@collectLatest pathfindingController.clear()
                    }

                    pathfindingController.findPath(
                        startingPoint = IntOffset(x = markerDescriptor.x, y = markerDescriptor.y),
                        endingPoint = IntOffset(x = 592, y = 1095)
                    )
                }
        }
    }
}