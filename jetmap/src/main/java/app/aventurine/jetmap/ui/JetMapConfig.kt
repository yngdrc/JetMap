package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.controller.VisibleArea

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

    fun getCoordinates(visibleArea: VisibleArea): Coordinates {
        fun calculateX(x: Int): Int {
            return (x * tileSize)
        }

        fun calculateY(y: Int): Int {
            return (y * tileSize)
        }

        return Coordinates(
            startX = calculateX(x = visibleArea.first.first),
            startY = calculateY(y = visibleArea.second.first),
            endX = calculateX(x = visibleArea.first.last),
            endY = calculateY(y = visibleArea.second.last)
        )
    }

    data class Coordinates(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    )

    companion object {
        const val BASE_FLOOR_ID: Int = 7
    }
}