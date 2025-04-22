package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Size

data class JetMapConfig(
    val tileSize: Size,
    val mapSize: Size,
    val minZoom: Float = 0.1f,
    val maxZoom: Float = 10f
)