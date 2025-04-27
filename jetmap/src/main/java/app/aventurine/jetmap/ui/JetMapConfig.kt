package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize

data class JetMapConfig(
    val tileSize: Int,
    val mapSize: IntSize,
    val minZoom: Float = 0.1f,
    val maxZoom: Float = 10f
) {
    val xTileCount: Int = mapSize.width / tileSize
    val yTileCount: Int = mapSize.height / tileSize
}