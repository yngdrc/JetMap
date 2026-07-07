package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.motion.VisibleArea

data class JetMapConfig(
    val tileSize: Int,
    val mapSize: IntSize,
    val minZoom: Float = 0.1f,
    val maxZoom: Float = 10f,
) {
    val xTileCount: Int = mapSize.width / tileSize
    val yTileCount: Int = mapSize.height / tileSize

    data class Coordinates(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    )
}