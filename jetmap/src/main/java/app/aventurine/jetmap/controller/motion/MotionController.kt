package app.aventurine.jetmap.controller.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal class MotionController(
    parentScope: CoroutineScope,
    private val config: JetMapConfig
) : MotionApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val _canvasSizeFlow: MutableStateFlow<IntSize> = MutableStateFlow(value = IntSize.Zero)
    override val canvasSizeFlow: StateFlow<IntSize> = _canvasSizeFlow.asStateFlow()

    private var minZoom: Float = config.minZoom

    private val _motionStateFlow: MutableStateFlow<MotionState> = MutableStateFlow(
        value = MotionState(zoom = minZoom, rotation = 0f, centroid = Offset.Zero)
    )
    override val motionStateFlow: StateFlow<MotionState> = _motionStateFlow.asStateFlow()

    override val visibleAreaFlow: Flow<VisibleArea> = combine(
        flow = _motionStateFlow,
        flow2 = _canvasSizeFlow
    ) { motionState, canvasSize -> motionState to canvasSize }
        .filter { (_, canvasSize) -> canvasSize.width > 0 && canvasSize.height > 0 }
        .map { (motionState, canvasSize) ->
            motionState.getVisibleArea(
                canvasSize = canvasSize,
                tileSize = config.tileSize,
                margin = config.tilePrefetchMargin
            )
        }
        .distinctUntilChanged()

    private val _levelStateFlow = MutableStateFlow(value = config.initialLevel)
    override val levelStateFlow: StateFlow<Int> = _levelStateFlow.asStateFlow()
    override fun changeLevel(level: Int) = _levelStateFlow.update { level }

    private val _cameraModeFlow: MutableStateFlow<CameraMode> = MutableStateFlow(CameraMode.FREE)
    override val cameraModeFlow: StateFlow<CameraMode> = _cameraModeFlow.asStateFlow()
    override fun setCameraMode(cameraMode: CameraMode) = _cameraModeFlow.update { cameraMode }

    private var moveJob: Job? = null

    /**
     * Called whenever the composable is measured. Recomputes the zoom floor and either seeds the
     * initial camera (first valid size) or re-clamps the existing one, so a re-measure never
     * resets the user viewport.
     */
    internal fun onCanvasSizeChanged(canvasSize: IntSize) {
        if (canvasSize == _canvasSizeFlow.value) {
            return
        }

        val previousSize = _canvasSizeFlow.value
        val wasInitialized = previousSize.width > 0 && previousSize.height > 0
        _canvasSizeFlow.value = canvasSize

        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            return
        }

        minZoom = calculateInitialZoom(canvasSize = canvasSize, config = config)

        if (!wasInitialized) {
            _motionStateFlow.value = MotionState(
                zoom = minZoom,
                rotation = 0f,
                centroid = calculateInitialCentroid(
                    canvasSize = canvasSize,
                    minZoom = minZoom,
                    mapSize = config.mapSize
                )
            )
            return
        }

        _motionStateFlow.update { motionState ->
            motionState
                .copy(zoom = motionState.zoom.coerceIn(minZoom, config.maxZoom))
                .clamped()
        }
    }

    internal fun onGestureStart() {
        moveJob?.cancel()
        if (_cameraModeFlow.value != CameraMode.FREE) {
            _cameraModeFlow.value = CameraMode.FREE
        }
    }

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
            ).clamped()
        }
    }

    /**
     * Pan inertia. [velocity] is in screen pixels per second, as reported by the velocity tracker.
     */
    internal fun onFling(velocity: Velocity) {
        if (!config.flingEnabled) {
            return
        }

        if (hypot(velocity.x, velocity.y) < MIN_FLING_VELOCITY) {
            return
        }

        moveJob?.cancel()
        moveJob = scope.launch {
            var previous = Offset.Zero
            AnimationState(
                typeConverter = Offset.VectorConverter,
                initialValue = Offset.Zero,
                initialVelocityVector = AnimationVector2D(velocity.x, velocity.y)
            ).animateDecay(animationSpec = exponentialDecay(frictionMultiplier = 1.4f)) {
                val delta = value - previous
                previous = value
                _motionStateFlow.update { motionState ->
                    motionState
                        .copy(centroid = motionState.centroid - delta / motionState.zoom)
                        .clamped()
                }
            }
        }
    }

    /**
     * Double tap zoom around the tapped screen point.
     */
    internal fun onDoubleTap(screenOffset: Offset) {
        val state = _motionStateFlow.value
        val targetZoom = (state.zoom * config.doubleTapZoomFactor)
            .coerceIn(minZoom, config.maxZoom)

        if (abs(targetZoom - state.zoom) < ZOOM_EPSILON) {
            return
        }

        animateZoomAround(pivot = screenOffset, targetZoom = targetZoom)
    }

    private fun animateZoomAround(pivot: Offset, targetZoom: Float) {
        moveJob?.cancel()
        val startState = _motionStateFlow.value
        moveJob = scope.launch {
            withContext(AndroidUiDispatcher.Main) {
                Animatable(initialValue = 0f).animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) {
                    val zoom = lerp(start = startState.zoom, stop = targetZoom, fraction = value)
                        .coerceIn(minZoom, config.maxZoom)

                    // Keeps the map point under the pivot fixed on screen.
                    val centroid = startState.centroid + pivot / startState.zoom - pivot / zoom
                    _motionStateFlow.update { motionState ->
                        motionState.copy(zoom = zoom, centroid = centroid).clamped()
                    }
                }
            }
        }
    }

    internal fun transformCanvas(
        motionState: MotionState
    ): DrawTransform.() -> Unit = {
        translate(
            left = -motionState.centroid.x * motionState.zoom,
            top = -motionState.centroid.y * motionState.zoom
        )

        scale(scaleX = motionState.zoom, scaleY = motionState.zoom, pivot = Offset.Zero)
        rotate(degrees = motionState.rotation, pivot = Offset.Zero)
    }

    override fun moveTo(
        offset: Offset,
        level: Int,
        zoom: Float?,
        rotation: Float?,
        animate: Boolean
    ) {
        val state = _motionStateFlow.value
        val targetZoom = (zoom ?: state.zoom).coerceIn(minZoom, config.maxZoom)
        val targetRotation = rotation ?: state.rotation
        val canvasCenter = canvasCenter()

        moveJob?.cancel()
        _levelStateFlow.update { level }

        if (!animate) {
            _motionStateFlow.update {
                MotionState(
                    zoom = targetZoom,
                    rotation = targetRotation,
                    centroid = offset.rotateBy(angle = targetRotation) - canvasCenter / targetZoom
                ).clamped()
            }
            return
        }

        val startZoom = state.zoom
        val startRotation = state.rotation
        val startMapCenter = (state.centroid + canvasCenter / startZoom)
            .rotateBy(angle = -state.rotation)

        moveJob = scope.launch {
            withContext(AndroidUiDispatcher.Main) {
                Animatable(initialValue = 0f).animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                ) {
                    val currentZoom = lerp(startZoom, targetZoom, value)
                        .coerceIn(minZoom, config.maxZoom)

                    val currentRotation = lerpAngle(startRotation, targetRotation, value)
                    val currentMapCenter = lerp(startMapCenter, offset, value)

                    _motionStateFlow.update {
                        MotionState(
                            zoom = currentZoom,
                            rotation = currentRotation,
                            centroid = currentMapCenter.rotateBy(angle = currentRotation) -
                                    canvasCenter / currentZoom
                        ).clamped()
                    }
                }
            }
        }
    }

    override fun fitBounds(
        bounds: Rect,
        level: Int,
        paddingLeft: Float,
        paddingTop: Float,
        paddingRight: Float,
        paddingBottom: Float,
        animate: Boolean
    ) {
        val canvasSize = _canvasSizeFlow.value
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            return
        }

        val availableWidth = (canvasSize.width - paddingLeft - paddingRight).coerceAtLeast(1f)
        val availableHeight = (canvasSize.height - paddingTop - paddingBottom).coerceAtLeast(1f)

        val targetZoom = min(
            availableWidth / max(bounds.width, 1f),
            availableHeight / max(bounds.height, 1f)
        ).coerceIn(minZoom, config.maxZoom)

        // Shifts the target so the route is centered inside the visible part of the viewport.
        val paddingShift = Offset(
            x = (paddingLeft - paddingRight) / 2f / targetZoom,
            y = (paddingTop - paddingBottom) / 2f / targetZoom
        )

        moveTo(
            offset = bounds.center - paddingShift,
            level = level,
            zoom = targetZoom,
            rotation = 0f,
            animate = animate
        )
    }

    /**
     * Navigation camera. Keeps [position] visible, optionally rotating the map so the travel
     * direction points up and shifting the position towards the lower part of the screen.
     */
    internal fun followPosition(
        position: Offset,
        bearingDegrees: Float?,
        level: Int,
        zoom: Float
    ) {
        val canvasSize = _canvasSizeFlow.value
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            return
        }

        val mode = _cameraModeFlow.value
        if (mode != CameraMode.FOLLOW && mode != CameraMode.FOLLOW_BEARING) {
            return
        }

        val targetZoom = zoom.coerceIn(minZoom, config.maxZoom)
        val bearing = bearingDegrees
        val useBearing = mode == CameraMode.FOLLOW_BEARING && bearing != null
        val targetRotation = if (useBearing && bearing != null) -bearing else 0f

        val target = if (useBearing && bearing != null) {
            val forward = Offset(x = 1f, y = 0f).rotateBy(angle = bearing)
            position + forward * (canvasSize.height * FOLLOW_FORWARD_FRACTION / targetZoom)
        } else {
            position
        }

        val canvasCenter = canvasCenter()
        moveJob?.cancel()
        _levelStateFlow.update { level }

        val start = _motionStateFlow.value
        val startMapCenter = (start.centroid + canvasCenter / start.zoom)
            .rotateBy(angle = -start.rotation)

        moveJob = scope.launch {
            withContext(AndroidUiDispatcher.Main) {
                Animatable(initialValue = 0f).animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) {
                    val currentZoom = lerp(start.zoom, targetZoom, value)
                        .coerceIn(minZoom, config.maxZoom)
                    val currentRotation = lerpAngle(start.rotation, targetRotation, value)
                    val currentCenter = lerp(startMapCenter, target, value)

                    _motionStateFlow.update {
                        MotionState(
                            zoom = currentZoom,
                            rotation = currentRotation,
                            centroid = currentCenter.rotateBy(angle = currentRotation) -
                                    canvasCenter / currentZoom
                        )
                    }
                }
            }
        }
    }

    private fun canvasCenter(): Offset {
        val canvasSize = _canvasSizeFlow.value
        return Offset(x = canvasSize.width / 2f, y = canvasSize.height / 2f)
    }

    /**
     * Keeps the viewport center inside the map. When the map is smaller than the viewport on an
     * axis, the map is centered on that axis instead.
     */
    private fun MotionState.clamped(): MotionState {
        if (!config.clampToBounds) {
            return this
        }

        val canvasSize = _canvasSizeFlow.value
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            return this
        }

        val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val mapCenter = (centroid + canvasCenter / zoom).rotateBy(angle = -rotation)

        val halfWidth = canvasSize.width / 2f / zoom
        val halfHeight = canvasSize.height / 2f / zoom
        val mapWidth = config.mapSize.width.toFloat()
        val mapHeight = config.mapSize.height.toFloat()

        val clampedX = if (halfWidth * 2f >= mapWidth) {
            mapWidth / 2f
        } else {
            mapCenter.x.coerceIn(halfWidth, mapWidth - halfWidth)
        }

        val clampedY = if (halfHeight * 2f >= mapHeight) {
            mapHeight / 2f
        } else {
            mapCenter.y.coerceIn(halfHeight, mapHeight - halfHeight)
        }

        if (clampedX == mapCenter.x && clampedY == mapCenter.y) {
            return this
        }

        return copy(
            centroid = Offset(clampedX, clampedY).rotateBy(angle = rotation) - canvasCenter / zoom
        )
    }

    private companion object {
        const val MIN_FLING_VELOCITY = 80f
        const val ZOOM_EPSILON = 0.001f
        const val FOLLOW_FORWARD_FRACTION = 0.2f

        /** Interpolates angles over the shortest arc so the camera never spins the long way. */
        fun lerpAngle(start: Float, stop: Float, fraction: Float): Float {
            var delta = (stop - start) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            return start + delta * fraction
        }
    }
}

