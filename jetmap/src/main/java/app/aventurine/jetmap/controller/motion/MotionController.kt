package app.aventurine.jetmap.controller.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MotionController(
    parentScope: CoroutineScope,
    private val canvasSize: IntSize,
    config: JetMapConfig
) : MotionApi {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    internal val minZoom: Float
    internal val maxZoom: Float = config.maxZoom
    internal val initialCentroid: Offset

    private var moveJob: Job? = null

    init {
        val canvasSizeBasedZoom = canvasSize.toSize().maxDimension /
                config.mapSize.toSize().minDimension

        minZoom = canvasSizeBasedZoom.coerceIn(
            minimumValue = config.minZoom,
            maximumValue = maxZoom
        )

        initialCentroid = Offset(
            x = -canvasSize.width / minZoom / 2f,
            y = -canvasSize.height / minZoom / 2f
        ) + Offset(
            x = config.mapSize.width / 2f,
            y = config.mapSize.height / 2f
        )
    }

    private val _motionState: MutableStateFlow<MotionState> = MutableStateFlow(
        MotionState(
            zoom = minZoom,
            rotation = 0f,
            centroid = initialCentroid
        )
    )

    override val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    override val visibleAreaFlow: Flow<VisibleArea> = _motionState.map { motionState ->
        motionState.getVisibleArea(
            canvasSize = canvasSize,
            tileSize = config.tileSize
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private val _levelState = MutableStateFlow(7)
    override val levelState: StateFlow<Int> = _levelState.asStateFlow()
    override fun changeLevel(level: Int) = _levelState.update { level }

    internal fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        moveJob?.cancel()
        _motionState.update { motionState ->
            val newZoom = (motionState.zoom * zoom).coerceIn(minZoom, maxZoom)
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

    override fun moveTo(offset: Offset, zoom: Float?) {
        val state = _motionState.value
        val targetZoom = (zoom ?: state.zoom).coerceIn(minZoom, maxZoom)
        val startZoom = state.zoom
        val canvasCenter = canvasSize.center.toOffset()
        val startMapCenter = (state.centroid + canvasCenter / startZoom).rotateBy(-state.rotation)

        moveJob?.cancel()
        moveJob = scope.launch {
            withContext(AndroidUiDispatcher.Main) {
                Animatable(0f).animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                ) {
                    val currentZoom = lerp(startZoom, targetZoom, value).coerceIn(minZoom, maxZoom)
                    val currentMapCenter = lerp(startMapCenter, offset, value)

                    _motionState.update {
                        it.copy(
                            zoom = currentZoom,
                            centroid = currentMapCenter.rotateBy(state.rotation) - canvasCenter / currentZoom
                        )
                    }
                }
            }
        }
    }
}