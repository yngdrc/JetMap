package app.aventurine.jetmap.provider

import app.aventurine.jetmap.controller.tile.TerrainType
import java.io.InputStream

interface TileProvider {
    fun getTileInputStream(
        x: Int,
        y: Int,
        z: Int,
        terrainType: TerrainType
    ): InputStream?
}