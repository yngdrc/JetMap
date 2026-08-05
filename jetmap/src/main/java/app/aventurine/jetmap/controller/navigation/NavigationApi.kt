package app.aventurine.jetmap.controller.navigation

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.controller.motion.CameraMode
import kotlinx.coroutines.flow.StateFlow

interface NavigationApi {
    val navigationStateFlow: StateFlow<NavigationState>
    val cameraModeFlow: StateFlow<CameraMode>

    /** Calculates a route and moves to [NavigationState.Preview] (Google Maps route preview). */
    fun findRoute(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    )

    /** Starts turn-by-turn guidance for the previewed route. */
    fun startNavigation()

    /** Leaves guidance and clears the route. */
    fun stopNavigation()

    /** Feeds a new user position (map units + level) into the guidance engine. */
    fun updatePosition(x: Float, y: Float, level: Int)

    /** Frames the whole route. */
    fun showOverview()

    /** Re-attaches the camera to the user position. */
    fun recenter(withBearing: Boolean = true)

    /** Moves the camera to the maneuver of the given step. */
    fun focusStep(stepIndex: Int)

    /**
     * Screen padding (px) that is covered by UI (bottom sheet, banners). Route framing keeps the
     * route inside the visible part, exactly like Google Maps does.
     */
    fun setViewportPadding(left: Float, top: Float, right: Float, bottom: Float)
}

