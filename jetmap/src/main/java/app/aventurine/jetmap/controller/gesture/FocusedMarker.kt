package app.aventurine.jetmap.controller.gesture

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset

data class FocusedMarker(
    val offset: Offset,
    val description: String,
    val exists: Boolean,
    @field:DrawableRes val iconId: Int?
)