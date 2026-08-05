package app.aventurine.jetmap.controller.gesture.models

import androidx.compose.ui.geometry.Offset

/**
 * A tap already converted from screen space to map space.
 */
data class TapEvent(
    val mapOffset: Offset,
    val level: Int,
    val tapArea: Float
)

