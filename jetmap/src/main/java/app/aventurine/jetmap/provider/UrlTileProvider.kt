package app.aventurine.jetmap.provider

import java.io.InputStream
import java.net.URL

class UrlTileProvider(
    private val tileSize: Int
) : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        z: Int
    ): InputStream {
        return URL("https://aventurine.app/api/tiles?x=${x * tileSize + 31744}&y=${y * tileSize + 30976}&floorId=$z").openStream()
    }
}