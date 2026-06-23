package app.aventurine.jetmap.controller.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class MotionController(
    parentScope: CoroutineScope,
    canvasSize: IntSize,
    config: JetMapConfig
) : MotionApi {
    internal val minZoom: Float
    internal val maxZoom: Float = config.maxZoom
    internal val initialCentroid: Offset

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

    private val _levelState = MutableStateFlow(7)
    private val _motionState: MutableStateFlow<MotionState> = MutableStateFlow(
        MotionState(
            zoom = minZoom,
            rotation = 0f,
            centroid = initialCentroid
        )
    )

    override val levelState: StateFlow<Int> = _levelState.asStateFlow()
    override val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    override val visibleAreaFlow: Flow<VisibleArea> = _motionState.map { motionState ->
        motionState.getVisibleArea(
            canvasSize = canvasSize,
            tileSize = config.tileSize
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    internal fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
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

    override fun changeLevel(
        levelUpdateScope: (Int) -> Int
    ) {
        _levelState.update { currentLevel ->
            levelUpdateScope(currentLevel)
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
}