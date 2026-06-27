package app.aventurine.jetmap.models

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import app.aventurine.jetmap.ui.JetMapConfig

data class Marker(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap,
    @field:DrawableRes val iconId: Int,
    val description: String
)