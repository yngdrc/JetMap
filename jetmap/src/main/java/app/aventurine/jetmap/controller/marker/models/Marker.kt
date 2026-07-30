package app.aventurine.jetmap.controller.marker.models

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Rect

data class Marker(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap,
    val description: String
) {
    val id: String = "${x}_${y}_${z}"
}