package app.aventurine.jetmapdemo.utils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import app.aventurine.jetmap.controller.tile.TerrainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MinimapStitcher(
    context: Context,
    private val folder: File = context.filesDir,
    private val filePrefix: String = "${TerrainType.NORMAL.name}_",
) {
    private data class TileInfo(
        val worldX: Int,
        val worldY: Int,
        val floor: Int,
        val fileName: String
    )

    /**
     * Wczytuje wszystkie kafelki dla danego [floor] i scala je w jedną bitmapę.
     * [sampleSize] pozwala zmniejszyć wynikową bitmapę (1 = pełna jakość, 2 = ½, 4 = ¼ itd.)
     */
    suspend fun stitch(floor: Int, sampleSize: Int = 1): Bitmap? = withContext(Dispatchers.IO) {
        val tiles = listTiles(floor)
        if (tiles.isEmpty()) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val minX = tiles.minOf { it.worldX }
        val minY = tiles.minOf { it.worldY }
        val maxX = tiles.maxOf { it.worldX }
        val maxY = tiles.maxOf { it.worldY }

        // Ustal rozmiar kafelka z pierwszego pliku
        val probeBitmap = File(
            folder,
            tiles.first().fileName
        ).inputStream().use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@withContext null

        val tileW = probeBitmap.width
        val tileH = probeBitmap.height

        val canvasWidth = (maxX - minX) / sampleSize + tileW
        val canvasHeight = (maxY - minY) / sampleSize + tileH

        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Narysuj pierwszy kafelek (już załadowany)
        canvas.drawBitmap(
            probeBitmap,
            ((tiles.first().worldX - minX) / sampleSize).toFloat(),
            ((tiles.first().worldY - minY) / sampleSize).toFloat(),
            null
        )
        probeBitmap.recycle()

        // Narysuj pozostałe kafelki
        for (tile in tiles.drop(1)) {
            val bitmap = File(
                folder,
                tile.fileName
            ).inputStream().use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: continue

            canvas.drawBitmap(
                bitmap,
                ((tile.worldX - minX) / sampleSize).toFloat(),
                ((tile.worldY - minY) / sampleSize).toFloat(),
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
            worldX = parts[0].toIntOrNull() ?: return null,
            worldY = parts[1].toIntOrNull() ?: return null,
            floor = parts[2].toIntOrNull() ?: return null,
            fileName = fileName
        )
    }
}