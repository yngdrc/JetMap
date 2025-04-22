package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Offset

data class MotionState(
    val zoom: Float = 1f,
    val rotation: Float = 0f,
    val centroid: Offset = Offset.Zero
)