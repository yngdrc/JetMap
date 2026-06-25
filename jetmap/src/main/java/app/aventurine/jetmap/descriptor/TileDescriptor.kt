package app.aventurine.jetmap.descriptor

import app.aventurine.jetmap.controller.tile.TerrainType

data class TileDescriptor(
    val x: Int,
    val y: Int,
    val z: Int,
    val terrainType: TerrainType
)