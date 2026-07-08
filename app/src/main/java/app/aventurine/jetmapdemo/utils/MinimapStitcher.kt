package app.aventurine.jetmapdemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmap.domain.models.MapConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

class MinimapStitcher(
    context: Context,
    private val mapConfig: MapConfigEntity,
    private val folder: File = context.filesDir,
    private val filePrefix: String = "${TerrainType.NORMAL.name}_",
) {
    private data class TileInfo(
        val x: Int,
        val y: Int,
        val floor: Int,
        val fileName: String
    )

    suspend fun stitch(
        floor: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val tiles = listTiles(floor)
        if (tiles.isEmpty()) return@withContext null

        val result = createBitmap(mapConfig.width, mapConfig.height)
        val canvas = Canvas(result)

        for (tile in tiles) {
            val bitmap = File(
                folder,
                tile.fileName
            ).inputStream().use {
                BitmapFactory.decodeStream(it, null, null)
            } ?: continue

            canvas.drawBitmap(
                bitmap,
                (tile.x - mapConfig.minX).toFloat(),
                (tile.y - mapConfig.minY).toFloat(),
                null
            )

            bitmap.recycle()
        }

        result
    }

    private fun listTiles(floor: Int): List<TileInfo> =
        folder.listFiles()
            ?.mapNotNull { parseFileName(it.name) }
            ?.filter { it.floor == floor }
            ?: emptyList()

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