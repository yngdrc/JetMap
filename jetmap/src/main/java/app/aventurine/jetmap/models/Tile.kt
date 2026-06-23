package app.aventurine.jetmap.models

import android.graphics.Bitmap

data class Tile(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap
)