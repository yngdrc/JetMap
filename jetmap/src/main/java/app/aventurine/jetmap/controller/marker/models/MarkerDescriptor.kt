package app.aventurine.jetmap.controller.marker.models

import androidx.annotation.DrawableRes

data class MarkerDescriptor(
    val x: Int,
    val y: Int,
    val z: Int,
    @field:DrawableRes val iconId: Int?,
    val description: String
) {
    val id: String = "${x}_${y}_${z}"
}