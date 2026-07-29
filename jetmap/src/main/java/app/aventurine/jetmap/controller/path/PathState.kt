package app.aventurine.jetmap.controller.path

import androidx.compose.ui.unit.IntOffset

sealed class PathState {
    data object FindingRoute : PathState()
    data object RouteNotFound : PathState()
    data class RouteFound(
        val startingPoint: IntOffset,
        val endingPoint: IntOffset,
        val pathData: Map<Int, List<List<IntOffset>>>
    ) : PathState()
}