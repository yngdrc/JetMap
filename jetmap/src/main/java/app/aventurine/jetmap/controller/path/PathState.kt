package app.aventurine.jetmap.controller.path

import androidx.compose.ui.unit.IntOffset

data class PathState(
    val startingPoint: IntOffset,
    val endingPoint: IntOffset,
    val pathData: List<IntOffset>
)