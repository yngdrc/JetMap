package app.aventurine.jetmap.provider

import android.content.res.AssetManager

class AssetTileProvider(
    private val tileSize: Int,
    private val assetManager: AssetManager
) : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        z: Int
    ) = assetManager.open(
        "minimap/Minimap_Color_${x * tileSize + 31744}_${y * tileSize + 30976}_$z.png"
    )
}