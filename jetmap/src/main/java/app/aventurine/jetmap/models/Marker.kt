package app.aventurine.jetmap.models

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import app.aventurine.jetmap.ui.JetMapConfig

data class Marker(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap,
    val description: String
) {
//    fun calculateX(mapConfig: JetMapConfig): Double {
//        return ((x.toDouble() - mapConfig.leftMostTileCoordinate) / mapConfig.mapWidth)
//    }
//
//    fun calculatedY(mapConfig: JetMapConfig): Double {
//        return ((y.toDouble() - mapConfig.topMostTileCoordinate) / mapConfig.mapHeight)
//    }
}