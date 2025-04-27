package app.aventurine.jetmapdemo

import android.content.res.AssetManager
import app.aventurine.jetmap.ui.TileProvider
import kotlinx.parcelize.Parcelize
import java.io.InputStream

@Parcelize
class TileProvider(
    private val tileSize: Int
) : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): InputStream? {
        return try {
            assetManager.open("minimap/Minimap_Color_${x * tileSize + 31744}_${y * tileSize + 30976}_7.png")
        } catch (e: Exception) {
            null
        }
    }
}