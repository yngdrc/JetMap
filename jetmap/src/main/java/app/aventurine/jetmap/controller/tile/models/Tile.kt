package app.aventurine.jetmap.controller.tile.models

import android.graphics.Bitmap
import app.aventurine.jetmap.controller.tile.TerrainType

data class Tile(
    val x: Int,
    val y: Int,
    val z: Int,
    val bitmap: Bitmap,
    val terrainType: TerrainType
) {
    val id: String = "${x}_${y}_${z}"
    val key: String = "${terrainType.name}_${x}_${y}_${z}"
    val positionKey: String = "${x}_${y}"
}