package app.aventurine.jetmapdemo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.aventurine.jetmap.ui.TileProvider
import kotlinx.parcelize.Parcelize

@Parcelize
class TileProvider : TileProvider {
    override fun getTileBitmap(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): Bitmap? {
        return try {
            assetManager.open("minimap/Minimap_Color_${x + 31744}_${y + 30976}_7.png")
                .use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
        } catch (e: Exception) {
            null
        }
    }
}