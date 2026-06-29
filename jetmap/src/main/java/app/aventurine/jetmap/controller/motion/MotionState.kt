package app.aventurine.jetmap.controller.motion

import androidx.compose.ui.geometry.Offset

data class MotionState(
    val zoom: Float,
    val rotation: Float,
    val centroid: Offset
)