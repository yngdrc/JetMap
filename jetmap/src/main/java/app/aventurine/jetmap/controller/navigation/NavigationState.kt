package app.aventurine.jetmap.controller.navigation

import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RoutePoint
import app.aventurine.jetmap.controller.navigation.models.RouteStep

sealed interface NavigationState {
    data object Idle : NavigationState

    data object Calculating : NavigationState

    data object RouteNotFound : NavigationState

    /** Route found, waiting for the user to press "Start". */
    data class Preview(val route: Route) : NavigationState

    /** Turn-by-turn guidance in progress. */
    data class Navigating(
        val route: Route,
        val currentStep: RouteStep,
        val currentStepIndex: Int,
        val nextStep: RouteStep?,
        val distanceToNextManeuver: Float,
        val remainingDistance: Float,
        val remainingSeconds: Float,
        val traveledPointIndex: Int,
        val snappedPosition: RoutePoint,
        val bearingDegrees: Float?,
        val isOffRoute: Boolean
    ) : NavigationState

    data class Arrived(val route: Route) : NavigationState
}

