package app.aventurine.jetmap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.utils.mapToScreen

/**
 * Renders the map.
 *
 * The composable owns no state: the canvas size is pushed to the controller from the layout phase
 * (`onSizeChanged`), never from measure, so a re-layout can not reset the camera.
 */
@Composable
fun JetMap(
    modifier: Modifier = Modifier,
    jetMapController: JetMapController,
    routeStyle: RouteStyle = RouteStyle(),
    backgroundColorSelector: (Int) -> Color = { Color.White }
) {
    val motionState by jetMapController.motionController.motionStateFlow
        .collectAsStateWithLifecycle()

    val tileState by jetMapController.tileController.tileStateFlow
        .collectAsStateWithLifecycle()

    val markerState by jetMapController.markerController.markerStateFlow
        .collectAsStateWithLifecycle()

    val navigationState by jetMapController.navigationController.navigationStateFlow
        .collectAsStateWithLifecycle()

    val level by jetMapController.motionController.levelStateFlow
        .collectAsStateWithLifecycle()

    val focusedMarker by jetMapController.gestureController.focusedMarkerFlow
        .collectAsStateWithLifecycle()

    val uiState by jetMapController.uiController.uiState

    // Gesture callbacks must always see the latest state, otherwise they capture the first one.
    val currentMotionState by rememberUpdatedState(newValue = motionState)
    val currentLevel by rememberUpdatedState(newValue = level)

    val tapAreaPx = with(LocalDensity.current) { TAP_AREA_DP.dp.toPx() }

    val route: Route? = navigationState.currentRoute
    val traveledIndex = (navigationState as? NavigationState.Navigating)?.traveledPointIndex ?: 0

    // Paths are rebuilt only when the route, the floor or the progress changes, never per frame.
    val routePaths = remember(route, level, traveledIndex) {
        route?.let {
            buildRoutePaths(
                route = it,
                level = level,
                traveledIndex = traveledIndex,
                arrowSpacing = routeStyle.arrowSpacingPx / motionState.zoom.coerceAtLeast(0.0001f)
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColorSelector(level))
            .clipToBounds()
            .onSizeChanged { size -> jetMapController.onCanvasSizeChanged(canvasSize = size) }
            .mapGestures(
                onGestureStart = jetMapController.motionController::onGestureStart,
                onGesture = jetMapController.motionController::onGesture,
                onFling = jetMapController.motionController::onFling,
                onTap = { offset ->
                    jetMapController.gestureController.onTap(
                        tapArea = tapAreaPx / currentMotionState.zoom.coerceAtLeast(0.0001f),
                        offset = offset,
                        motionState = currentMotionState,
                        level = currentLevel
                    )
                },
                onDoubleTap = jetMapController.motionController::onDoubleTap
            ),
        onDraw = {
            withTransform(
                transformBlock = jetMapController.motionController.transformCanvas(
                    motionState = motionState
                )
            ) {
                drawIntoCanvas { canvas ->
                    jetMapController.tileController.draw(
                        tileState = tileState,
                        canvas = canvas
                    )
                }

                routePaths?.let { paths ->
                    drawRoute(paths = paths, style = routeStyle, zoom = motionState.zoom)
                }

                drawIntoCanvas { canvas ->
                    jetMapController.markerController.draw(
                        markers = markerState,
                        focusedMarker = focusedMarker,
                        canvas = canvas,
                        showMarkers = uiState.showMarkers
                    )
                }
            }

            // Screen space overlays keep a constant size regardless of zoom.
            route?.let { currentRoute ->
                drawRouteEndpoints(
                    originScreen = currentRoute.origin
                        .takeIf { it.level == level }
                        ?.let { motionState.mapToScreen(mapOffset = it.offset) },
                    destinationScreen = currentRoute.destination
                        .takeIf { it.level == level }
                        ?.let { motionState.mapToScreen(mapOffset = it.offset) },
                    style = routeStyle
                )
            }

            (navigationState as? NavigationState.Navigating)
                ?.takeIf { it.snappedPosition.level == level }
                ?.let { navigating ->
                    drawUserPosition(
                        positionScreen = motionState.mapToScreen(
                            mapOffset = navigating.snappedPosition.offset
                        ),
                        bearingDegrees = (navigating.bearingDegrees ?: 0f) + motionState.rotation
                    )
                }
        }
    )
}

private val NavigationState.currentRoute: Route?
    get() = when (this) {
        is NavigationState.Preview -> this.route
        is NavigationState.Navigating -> this.route
        is NavigationState.Arrived -> this.route
        else -> null
    }

private const val TAP_AREA_DP = 12

