package app.aventurine.jetmap.provider

import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import java.io.InputStream
import java.net.URL

class UrlTileProvider(
    private val tileSize: Int
) : TileProvider {
    override fun getTileInputStream(
        tileDescriptor: TileDescriptor
    ): InputStream {
        return URL("https://aventurine.app/api/tiles?x=${tileDescriptor.x * tileSize + 31744}&y=${tileDescriptor.y * tileSize + 30976}&floorId=${tileDescriptor.z}").openStream()
    }
}