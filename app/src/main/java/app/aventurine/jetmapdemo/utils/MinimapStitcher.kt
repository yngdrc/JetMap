package app.aventurine.jetmapdemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmap.domain.models.MapConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Builds the pathfinding [CostMap] of a floor directly from the minimap tiles.
 *
 * The previous implementation allocated a full ARGB_8888 bitmap of the whole floor
 * (8192x8192 = 256 MB) which reliably ran out of memory. Now only one tile is decoded at a time
 * and immediately folded into a one byte per cell grid.
 */
class MinimapStitcher(
    context: Context,
    private val mapConfig: MapConfigEntity,
    private val folder: File = context.filesDir,
    private val filePrefix: String = "${TerrainType.WAYPOINT_COST.name}_",
) {
    private data class TileInfo(
        val x: Int,
        val y: Int,
        val floor: Int,
        val fileName: String
    )

    /** Directory listing is expensive; index it once instead of on every route request. */
    @Volatile
    private var index: Map<Int, List<TileInfo>>? = null

    private val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        // Density scaling would shift the colours, and colour *is* the terrain cost here.
        inScaled = false
    }

    suspend fun buildCostMap(
        floor: Int
    ): CostMap? = withContext(Dispatchers.IO) {
        val tiles = tilesOf(floor = floor)
        if (tiles.isEmpty()) {
            return@withContext null
        }

        val costMap = CostMap(width = mapConfig.width, height = mapConfig.height)

        tiles.forEach { tile ->
            val bitmap = runCatching {
                File(folder, tile.fileName).inputStream().use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull() ?: return@forEach

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            costMap.writeTile(
                pixels = pixels,
                tileWidth = bitmap.width,
                tileHeight = bitmap.height,
                originX = tile.x - mapConfig.minX,
                originY = tile.y - mapConfig.minY
            )

            // Safe here: the bitmap is local and never handed to the renderer.
            bitmap.recycle()
        }

        costMap
    }

    private fun tilesOf(floor: Int): List<TileInfo> {
        val cached = index ?: buildIndex().also { index = it }
        return cached[floor].orEmpty()
    }

    private fun buildIndex(): Map<Int, List<TileInfo>> = folder.listFiles()
        ?.mapNotNull { file -> parseFileName(file.name) }
        ?.groupBy { it.floor }
        ?: emptyMap()

    private fun parseFileName(fileName: String): TileInfo? {
        if (!fileName.startsWith(filePrefix) || !fileName.endsWith(".png")) return null
        val parts = fileName.removePrefix(filePrefix).removeSuffix(".png").split("_")
        if (parts.size != 3) return null
        return TileInfo(
            x = parts[0].toIntOrNull() ?: return null,
            y = parts[1].toIntOrNull() ?: return null,
            floor = parts[2].toIntOrNull() ?: return null,
            fileName = fileName
        )
    }
}