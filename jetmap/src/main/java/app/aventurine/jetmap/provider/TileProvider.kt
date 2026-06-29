package app.aventurine.jetmap.provider

import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import java.io.InputStream

interface TileProvider {
    fun getTileInputStream(
        tileDescriptor: TileDescriptor
    ): InputStream?
}