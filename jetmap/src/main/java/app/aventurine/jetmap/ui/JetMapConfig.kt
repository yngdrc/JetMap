package app.aventurine.jetmap.ui

import androidx.compose.ui.unit.IntSize

data class JetMapConfig(
    val tileSize: Int,
    val mapSize: IntSize,
    val initialLevel: Int = 0,
    val minZoom: Float = 0.1f,
    val maxZoom: Float = 10f,
    /** Extra ring of tiles fetched around the visible area to hide loading while panning. */
    val tilePrefetchMargin: Int = 1,
    /** Max number of decoded tiles kept in memory (LRU). */
    val tileCacheSize: Int = 128,
    /** Max number of tiles decoded in parallel. */
    val tileParallelism: Int = 4,
    /** Enables pan inertia after a fling gesture. */
    val flingEnabled: Boolean = true,
    /** Zoom multiplier applied on double tap. */
    val doubleTapZoomFactor: Float = 2f,
    /** Keeps the camera inside the map bounds. */
    val clampToBounds: Boolean = true,
    /** Average travel speed in map units per second, used for ETA. */
    val navigationSpeedUnitsPerSecond: Float = 60f,
    /** Zoom used by the turn-by-turn camera. */
    val navigationZoom: Float = 4f,
    /** Distance from the polyline (map units) above which the user is considered off route. */
    val offRouteThreshold: Float = 12f,
    /** Distance to the destination (map units) at which arrival is triggered. */
    val arrivalThreshold: Float = 4f,
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