package app.aventurine.jetmap.controller.pathfinding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

data class PathState(
    val startingPoint: IntOffset,
    val endingPoint: IntOffset,
    val pathData: List<IntOffset>
)