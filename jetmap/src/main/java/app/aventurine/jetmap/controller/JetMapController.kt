package app.aventurine.jetmap.controller

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.gesture.GestureApi
import app.aventurine.jetmap.controller.gesture.GestureController
import app.aventurine.jetmap.controller.marker.MarkerController
import app.aventurine.jetmap.controller.motion.MotionApi
import app.aventurine.jetmap.controller.motion.MotionController
import app.aventurine.jetmap.controller.navigation.NavigationApi
import app.aventurine.jetmap.controller.navigation.NavigationController
import app.aventurine.jetmap.controller.tile.TileApi
import app.aventurine.jetmap.controller.tile.TileController
import app.aventurine.jetmap.controller.ui.UIApi
import app.aventurine.jetmap.controller.ui.UIController
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.getVisibleAreaRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns every sub controller and wires them together.
 *
 * The controller is created once and survives re-measures: the canvas size is pushed in through
 * [onCanvasSizeChanged] instead of recreating controllers, which previously reset the viewport and
 * leaked one collector per measure pass.
 */
@OptIn(FlowPreview::class)
class JetMapController(
    parentScope: CoroutineScope,
    tileProvider: TileProvider,
    markerProvider: MarkerProvider,
    pathProvider: PathProvider?,
    val config: JetMapConfig,
) {
    private val scope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob() + Dispatchers.Main.immediate
    )

    internal val motionController: MotionController = MotionController(
        parentScope = scope,
        config = config
    )

    internal val tileController: TileController = TileController(
        tileProvider = tileProvider,
        config = config
    )

    internal val markerController: MarkerController = MarkerController(
        markerProvider = markerProvider,
        config = config
    )

    internal val gestureController: GestureController = GestureController(
        markerProvider = markerProvider
    )

    internal val navigationController: NavigationController = NavigationController(
        parentScope = scope,
        pathProvider = pathProvider,
        motionController = motionController,
        config = config
    )

    internal val uiController: UIController = UIController()

    /** Public, interface typed facades. No casting, no leaking of the internal controllers. */
    val motionApi: MotionApi get() = motionController
    val tileApi: TileApi get() = tileController
    val gestureApi: GestureApi get() = gestureController
    val navigationApi: NavigationApi get() = navigationController
    val uiApi: UIApi get() = uiController

    private val _initializationState: MutableState<Boolean> = mutableStateOf(value = false)
    val initializationState: State<Boolean> = _initializationState

    init {
        collectStates()
    }

    /**
     * Idempotent. Safe to call from every layout pass.
     */
    fun onCanvasSizeChanged(canvasSize: IntSize) {
        motionController.onCanvasSizeChanged(canvasSize = canvasSize)
        if (canvasSize.width > 0 && canvasSize.height > 0 && !_initializationState.value) {
            _initializationState.value = true
        }
    }

    /**
     * Cancels every collector and animation. Call it from `ViewModel.onCleared`.
     */
    fun dispose() {
        scope.cancel()
    }

    private fun collectStates() {
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

        scope.launch {
            combine(
                flow = motionController.motionStateFlow.debounce(timeout = 300.milliseconds),
                flow2 = motionController.levelStateFlow,
                flow3 = motionController.canvasSizeFlow
            ) { motionState, level, canvasSize ->
                Triple(motionState, level, canvasSize)
            }.filter { (_, _, canvasSize) ->
                canvasSize.width > 0 && canvasSize.height > 0
            }.collectLatest { (motionState, level, canvasSize) ->
                markerController.onVisibleAreaChanged(
                    visibleAreaRect = motionState.getVisibleAreaRect(canvasSize = canvasSize),
                    level = level
                )
            }
        }

        scope.launch {
            gestureController.tapFlow.collectLatest { tapEvent ->
                gestureController.onMarkerFocusChanged(tapEvent = tapEvent)
            }
        }
    }
}
