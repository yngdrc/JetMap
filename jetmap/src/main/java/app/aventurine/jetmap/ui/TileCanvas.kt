package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

class TileCanvas(
    val tileProvider: TileProvider,
    val tileSize: Int
) {
    fun draw(
        canvas: Canvas,
        visibleArea: Rect,
        assetManager: AssetManager
    ) {
        val startX = visibleArea.left.toInt() / tileSize
        val endX = visibleArea.right.toInt() / tileSize
        val startY = visibleArea.top.toInt() / tileSize
        val endY = visibleArea.bottom.toInt() / tileSize

        for (x in startX..endX)
            for (y in startY..endY)
                tileProvider.getTileBitmap(
                    x = x * tileSize,
                    y = y * tileSize,
                    assetManager = assetManager
                )?.let { tileBitmap ->
                    canvas.nativeCanvas.drawBitmap(
                        tileBitmap,
                        x.toFloat() * tileSize,
                        y.toFloat() * tileSize,
                        null
                    )
                }
    }
}