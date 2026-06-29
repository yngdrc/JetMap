package app.aventurine.jetmap.controller.tile.models

import android.graphics.Bitmap
import app.aventurine.jetmap.controller.motion.VisibleArea

data class Tile(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap
) {
    val id: String = "${x}_${y}_${z}"
}