package app.aventurine.jetmap.controller.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.controller.motion.CameraMode
import app.aventurine.jetmap.controller.motion.MotionController
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RoutePoint
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmap.ui.JetMapConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Turn-by-turn navigation engine: route calculation, smoothing, progress tracking,
 * off-route detection with automatic recalculation and camera control.
 */
class NavigationController internal constructor(
    parentScope: CoroutineScope,
    private val pathProvider: PathProvider?,
    private val motionController: MotionController,
    private val config: JetMapConfig
) : NavigationApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val _navigationStateFlow: MutableStateFlow<NavigationState> =
        MutableStateFlow(value = NavigationState.Idle)

    override val navigationStateFlow: StateFlow<NavigationState> =
        _navigationStateFlow.asStateFlow()

    override val cameraModeFlow: StateFlow<CameraMode> = motionController.cameraModeFlow

    private var findJob: Job? = null
    private var lastRequest: Pair<Pair<IntOffset, Int>, Pair<IntOffset, Int>>? = null

    private var snapIndex: Int = 0
    private var offRouteSinceMillis: Long? = null
    private var rerouteAttempts: Int = 0

    private var paddingLeft = 0f
    private var paddingTop = 0f
    private var paddingRight = 0f
    private var paddingBottom = 0f

    override fun setViewportPadding(left: Float, top: Float, right: Float, bottom: Float) {
        paddingLeft = left
        paddingTop = top
        paddingRight = right
        paddingBottom = bottom
    }

    override fun findRoute(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ) {
        if (pathProvider == null) {
            _navigationStateFlow.value = NavigationState.RouteNotFound
            return
        }

        if (startingPoint == endingPoint) {
            _navigationStateFlow.value = NavigationState.RouteNotFound
            return
        }

        lastRequest = startingPoint to endingPoint
        findJob?.cancel()
        _navigationStateFlow.value = NavigationState.Calculating

        findJob = scope.launch {
            val route = try {
                withContext(Dispatchers.Default) {
                    val segments = pathProvider.getPath(
                        startingPoint = startingPoint,
                        endingPoint = endingPoint
                    ).filter { it.points.isNotEmpty() }

                    if (segments.isEmpty()) {
                        null
                    } else {
                        RouteBuilder.build(
                            segments = segments,
                            speedUnitsPerSecond = config.navigationSpeedUnitsPerSecond
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            if (route == null) {
                _navigationStateFlow.value = NavigationState.RouteNotFound
                return@launch
            }

            snapIndex = 0
            offRouteSinceMillis = null
            _navigationStateFlow.value = NavigationState.Preview(route = route)
            showOverview()
        }
    }

    override fun startNavigation() {
        val route = currentRoute() ?: return

        rerouteAttempts = 0
        snapIndex = 0
        offRouteSinceMillis = null
        motionController.setCameraMode(CameraMode.FOLLOW_BEARING)

        _navigationStateFlow.value = buildNavigatingState(
            route = route,
            snap = SnapResult(
                index = 0,
                fraction = 0f,
                position = route.origin.offset,
                distance = 0f
            ),
            level = route.origin.level,
            isOffRoute = false
        )

        followCamera(route = route, index = 0, position = route.origin.offset)
    }

    override fun stopNavigation() {
        findJob?.cancel()
        lastRequest = null
        snapIndex = 0
        offRouteSinceMillis = null
        rerouteAttempts = 0
        motionController.setCameraMode(CameraMode.FREE)
        _navigationStateFlow.value = NavigationState.Idle
    }

    override fun updatePosition(x: Float, y: Float, level: Int) {
        val state = _navigationStateFlow.value
        val route = when (state) {
            is NavigationState.Navigating -> state.route
            else -> return
        }

        val position = Offset(x = x, y = y)
        val snap = RouteMath.snapToRoute(
            route = route,
            position = position,
            level = level,
            aroundIndex = snapIndex
        ) ?: RouteMath.snapToRouteFully(route = route, position = position, level = level)
        ?: return

        snapIndex = snap.index

        val offRoute = snap.distance > config.offRouteThreshold
        val now = System.currentTimeMillis()

        if (offRoute) {
            val since = offRouteSinceMillis ?: now.also { offRouteSinceMillis = it }
            if (now - since >= OFF_ROUTE_GRACE_MILLIS && rerouteAttempts < MAX_REROUTE_ATTEMPTS) {
                rerouteAttempts++
                offRouteSinceMillis = null
                recalculate(fromX = x, fromY = y, level = level)
                return
            }
        } else {
            offRouteSinceMillis = null
            rerouteAttempts = 0
        }

        val navigating = buildNavigatingState(
            route = route,
            snap = snap,
            level = level,
            isOffRoute = offRoute
        )

        if (navigating.remainingDistance <= config.arrivalThreshold) {
            motionController.setCameraMode(CameraMode.FREE)
            _navigationStateFlow.value = NavigationState.Arrived(route = route)
            return
        }

        _navigationStateFlow.value = navigating
        followCamera(route = route, index = snap.index, position = snap.position)
    }

    override fun showOverview() {
        val route = currentRoute() ?: return
        motionController.setCameraMode(CameraMode.OVERVIEW)
        motionController.fitBounds(
            bounds = route.bounds.inflate(delta = OVERVIEW_INFLATE),
            level = route.origin.level,
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            paddingRight = paddingRight,
            paddingBottom = paddingBottom
        )
    }

    override fun recenter(withBearing: Boolean) {
        val state = _navigationStateFlow.value as? NavigationState.Navigating ?: return
        motionController.setCameraMode(
            if (withBearing) CameraMode.FOLLOW_BEARING else CameraMode.FOLLOW
        )
        followCamera(
            route = state.route,
            index = state.traveledPointIndex,
            position = state.snappedPosition.offset
        )
    }

    override fun focusStep(stepIndex: Int) {
        val route = currentRoute() ?: return
        val step = route.steps.getOrNull(stepIndex) ?: return
        val point = route.points.getOrNull(step.endIndex) ?: return

        motionController.setCameraMode(CameraMode.FREE)
        motionController.moveTo(
            offset = point.offset,
            level = step.level,
            zoom = config.navigationZoom,
            rotation = 0f
        )
    }

    private fun recalculate(fromX: Float, fromY: Float, level: Int) {
        val destination = currentRoute()?.destination ?: return
        findRoute(
            startingPoint = IntOffset(x = fromX.toInt(), y = fromY.toInt()) to level,
            endingPoint = IntOffset(
                x = destination.x.toInt(),
                y = destination.y.toInt()
            ) to destination.level
        )

        scope.launch {
            // Preview is emitted by findRoute; resume guidance automatically after a reroute.
            findJob?.join()
            if (_navigationStateFlow.value is NavigationState.Preview) {
                startNavigation()
            }
        }
    }

    private fun buildNavigatingState(
        route: Route,
        snap: SnapResult,
        level: Int,
        isOffRoute: Boolean
    ): NavigationState.Navigating {
        val stepIndex = route.steps.indexOfFirst { step ->
            snap.index < step.endIndex
        }.let { if (it == -1) route.steps.lastIndex else it }

        val currentStep = route.steps[stepIndex]
        val nextStep = route.steps.getOrNull(stepIndex + 1)

        val traveled = traveledDistance(route = route, snap = snap)
        val maneuverDistance = (route.cumulativeDistances[currentStep.endIndex] - traveled)
            .coerceAtLeast(0f)

        val remaining = (route.totalDistance - traveled).coerceAtLeast(0f)

        return NavigationState.Navigating(
            route = route,
            currentStep = currentStep,
            currentStepIndex = stepIndex,
            nextStep = nextStep,
            distanceToNextManeuver = maneuverDistance,
            remainingDistance = remaining,
            remainingSeconds = remaining / config.navigationSpeedUnitsPerSecond
                .coerceAtLeast(0.0001f),
            traveledPointIndex = snap.index,
            snappedPosition = RoutePoint(
                x = snap.position.x,
                y = snap.position.y,
                level = level
            ),
            bearingDegrees = bearingAt(route = route, index = snap.index),
            isOffRoute = isOffRoute
        )
    }

    private fun traveledDistance(route: Route, snap: SnapResult): Float {
        val from = route.cumulativeDistances[snap.index]
        val to = route.cumulativeDistances.getOrElse(snap.index + 1) { from }
        return from + (to - from) * snap.fraction
    }

    private fun bearingAt(route: Route, index: Int): Float? {
        val current = route.points.getOrNull(index) ?: return null
        val next = route.points.getOrNull(index + 1) ?: return null
        if (current.level != next.level) {
            return null
        }

        return RouteMath.bearing(from = current.offset, to = next.offset)
    }

    private fun followCamera(route: Route, index: Int, position: Offset) {
        motionController.followPosition(
            position = position,
            bearingDegrees = bearingAt(route = route, index = index),
            level = route.points.getOrNull(index)?.level ?: route.origin.level,
            zoom = config.navigationZoom
        )
    }

    private fun currentRoute(): Route? = when (val state = _navigationStateFlow.value) {
        is NavigationState.Preview -> state.route
        is NavigationState.Navigating -> state.route
        is NavigationState.Arrived -> state.route
        else -> null
    }

    private companion object {
        const val OFF_ROUTE_GRACE_MILLIS = 2_000L
        const val MAX_REROUTE_ATTEMPTS = 3
        const val OVERVIEW_INFLATE = 8f
    }
}

