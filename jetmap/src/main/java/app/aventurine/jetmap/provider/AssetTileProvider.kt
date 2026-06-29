package app.aventurine.jetmap.provider

import android.content.res.AssetManager
import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import java.io.InputStream

class AssetTileProvider(
    private val tileSize: Int,
    private val assetManager: AssetManager
) : TileProvider {
    override fun getTileInputStream(
        tileDescriptor: TileDescriptor
    ): InputStream {
        val fileNamePrefix = when (tileDescriptor.terrainType) {
            TerrainType.NORMAL -> "minimap/Minimap_Color_"
            TerrainType.PATHFINDING -> "minimap_pathfinding/Minimap_WaypointCost_"
        }

        return assetManager.open(
            "$fileNamePrefix${tileDescriptor.x * tileSize + 31744}_${tileDescriptor.y * tileSize + 30976}_${tileDescriptor.z}.png"
        )
    }
}