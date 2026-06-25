package app.aventurine.jetmap.provider

import android.content.res.AssetManager
import app.aventurine.jetmap.controller.tile.TerrainType
import java.io.InputStream

class AssetTileProvider(
    private val tileSize: Int,
    private val assetManager: AssetManager
) : TileProvider {
    override fun getTileInputStream(
        x: Int,
        y: Int,
        z: Int,
        terrainType: TerrainType
    ): InputStream {
        val fileNamePrefix = when (terrainType) {
            TerrainType.NORMAL -> "minimap/Minimap_Color_"
            TerrainType.PATHFINDING -> "minimap_pathfinding/Minimap_WaypointCost_"
        }

        return assetManager.open(
            "$fileNamePrefix${x * tileSize + 31744}_${y * tileSize + 30976}_$z.png"
        )
    }
}