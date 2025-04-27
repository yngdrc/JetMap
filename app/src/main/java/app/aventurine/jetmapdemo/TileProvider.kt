package app.aventurine.jetmapdemo

import android.content.res.AssetManager
import app.aventurine.jetmap.ui.TileProvider
import kotlinx.parcelize.Parcelize
import java.io.InputStream

@Parcelize
class TileProvider : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): InputStream? {
        return try {
            assetManager.open("minimap/Minimap_Color_${x + 31744}_${y + 30976}_7.png")
        } catch (e: Exception) {
            null
        }
    }
}