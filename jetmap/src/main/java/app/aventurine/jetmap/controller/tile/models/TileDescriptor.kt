package app.aventurine.jetmap.controller.tile.models

import app.aventurine.jetmap.controller.tile.TerrainType

data class TileDescriptor(
    val x: Int,
    val y: Int,
    val z: Int,
    val terrainType: TerrainType
) {
    val id: String = "${x}_${y}_${z}"

    /** Cache key. Includes the terrain so switching terrain never reuses the wrong bitmap. */
    val key: String = "${terrainType.name}_${x}_${y}_${z}"
}