package app.aventurine.jetmap.controller.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.lerp
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.calculateInitialCentroid
import app.aventurine.jetmap.utils.calculateInitialZoom
import app.aventurine.jetmap.utils.getVisibleArea
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MotionController(
    parentScope: CoroutineScope,
    private val canvasSize: IntSize,
    private val config: JetMapConfig
) : MotionApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val minZoom = calculateInitialZoom(canvasSize = canvasSize, config = config)

    private val _motionStateFlow: MutableStateFlow<MotionState> = MutableStateFlow(
        value = MotionState(
            zoom = minZoom,
            rotation = 0f,
            centroid = calculateInitialCentroid(
                canvasSize = canvasSize,
                minZoom = minZoom,
                mapSize = config.mapSize
            )
        )
    )

    override val motionStateFlow: StateFlow<MotionState> = _motionStateFlow.asStateFlow()

    override val visibleAreaFlow: Flow<VisibleArea> = _motionStateFlow.map { motionState ->
        motionState.getVisibleArea(
            canvasSize = canvasSize,
            tileSize = config.tileSize
        )
    }.distinctUntilChanged()

    private val _levelStateFlow = MutableStateFlow(value = 7)
    override val levelStateFlow: StateFlow<Int> = _levelStateFlow.asStateFlow()
    override fun changeLevel(level: Int) = _levelStateFlow.update { level }

    private var moveJob: Job? = null

    internal fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        moveJob?.cancel()
        _motionStateFlow.update { motionState ->
            val newZoom = (motionState.zoom * zoom).coerceIn(
                minimumValue = minZoom,
                maximumValue = config.maxZoom
            )

            val newCentroid = (motionState.centroid + centroid / motionState.zoom).rotateBy(
                angle = rotation
            ) - (centroid / newZoom + pan / motionState.zoom)

            motionState.copy(
                zoom = newZoom,
                rotation = motionState.rotation + rotation,
                centroid = newCentroid
            )
        }
    }

    internal fun transformCanvas(
        motionState: MotionState
    ): DrawTransform.() -> Unit = {
        translate(
            left = -motionState.centroid.x * motionState.zoom,
            top = -motionState.centroid.y * motionState.zoom
        )

        scale(scale = motionState.zoom, pivot = Offset.Zero)
        rotate(degrees = motionState.rotation, pivot = Offset.Zero)
    }

    override fun moveTo(offset: Offset, level: Int, zoom: Float?) {
        val state = _motionStateFlow.value
        val targetZoom = (zoom ?: state.zoom).coerceIn(minZoom, config.maxZoom)
        val startZoom = state.zoom
        val canvasCenter = canvasSize.center.toOffset()
        val startMapCenter = (state.centroid + canvasCenter / startZoom).rotateBy(-state.rotation)

        moveJob?.cancel()
        moveJob = scope.launch {
            _levelStateFlow.update { level }
            withContext(AndroidUiDispatcher.Main) {
                Animatable(initialValue = 0f).animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                ) {
                    val currentZoom = lerp(
                        start = startZoom,
                        stop = targetZoom,
                        fraction = value
                    ).coerceIn(minZoom, config.maxZoom)

                    val currentMapCenter = lerp(
                        start = startMapCenter,
                        stop = offset,
                        fraction = value
                    )

                    _motionStateFlow.update { motionState ->
                        motionState.copy(
                            zoom = currentZoom,
                            centroid = currentMapCenter.rotateBy(
                                angle = state.rotation
                            ) - canvasCenter / currentZoom
                        )
                    }
                }
            }
        }
    }
}