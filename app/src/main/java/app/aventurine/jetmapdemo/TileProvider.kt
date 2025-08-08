package app.aventurine.jetmapdemo

import android.content.res.AssetManager
import app.aventurine.jetmap.ui.TileProvider
import kotlinx.parcelize.Parcelize
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

@Parcelize
class TileProvider(
    private val tileSize: Int
) : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): InputStream {
        return URL("https://aventurine.app/api/tiles?x=${x * tileSize + 31744}&y=${y * tileSize + 30976}&floorId=7").openStream()
    }
}