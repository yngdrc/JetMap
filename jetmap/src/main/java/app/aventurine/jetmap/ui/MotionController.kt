package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

data class MotionState(
    val zoom: Float,
    val rotation: Float,
    val centroid: Offset
)

fun MotionState.getVisibleAreaRect(
    canvasSize: IntSize
): Rect {
    val rotatedCanvasRect = Rect(
        offset = Offset.Zero,
        size = canvasSize.toSize() / zoom
    ).rotateBy(angle = -rotation)

    return Rect(
        offset = centroid.rotateBy(
            angle = -rotation
        ) + rotatedCanvasRect.topLeft,
        size = rotatedCanvasRect.size / zoom
    )
}

fun MotionState.getVisibleArea(
    canvasSize: IntSize,
    tileSize: Int
): VisibleArea {
    val visibleAreaRect = getVisibleAreaRect(canvasSize = canvasSize)
    val startX = visibleAreaRect.left.toInt() / tileSize
    val endX = visibleAreaRect.right.toInt() / tileSize
    val startY = visibleAreaRect.top.toInt() / tileSize
    val endY = visibleAreaRect.bottom.toInt() / tileSize

    return VisibleArea(
        startX..endX,
        startY..endY
    )
}

typealias VisibleArea = Pair<IntRange, IntRange>

internal class MotionController(
    parentScope: CoroutineScope,
    canvasSize: IntSize,
    config: JetMapConfig
) {
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(parentScope.coroutineContext + singleThreadDispatcher)

    val minZoom: Float
    val maxZoom: Float = config.maxZoom
    val initialCentroid: Offset

    init {
        val canvasSizeBasedZoom = canvasSize.toSize().maxDimension /
                config.mapSize.toSize().minDimension

        minZoom = canvasSizeBasedZoom.coerceIn(
            minimumValue = config.minZoom,
            maximumValue = config.maxZoom
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

    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()
    val visibleAreaFlow: Flow<VisibleArea> = _motionState.map { motionState ->
        motionState.getVisibleArea(
            canvasSize = canvasSize,
            tileSize = config.tileSize
        )
    }.distinctUntilChanged().shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1
    )

    fun onGesture(
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
}