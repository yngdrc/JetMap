package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.motion.VisibleArea

data class JetMapConfig(
    val tileSize: Int,
    val mapSize: IntSize,
    val minZoom: Float = 0.1f,
    val maxZoom: Float = 10f,
) {
    constructor(
        tileSize: Int,
        leftMostTileCoordinate: Int,
        rightMostTileCoordinate: Int,
        topMostTileCoordinate: Int,
        bottomMostTileCoordinate: Int,
        minZoom: Float = 0.1f,
        maxZoom: Float = 10f
    ) : this(
        tileSize = tileSize,
        mapSize = IntSize(
            width = rightMostTileCoordinate - leftMostTileCoordinate + tileSize,
            height = bottomMostTileCoordinate - topMostTileCoordinate + tileSize
        ),
    )

    val xTileCount: Int = mapSize.width / tileSize
    val yTileCount: Int = mapSize.height / tileSize

    data class Coordinates(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    )

    companion object {
        val LEVELS = IntRange(0, 15)
        const val BASE_FLOOR_ID: Int = 7
    }
}