package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.fastDistinctBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TileState(
    val tiles: Collection<Tile>
)

class TileController(
    val tileProvider: TileProvider,
    val tileSize: Int
) {
    private val _tileState: MutableStateFlow<TileState> = MutableStateFlow(
        TileState(tiles = emptyList())
    )

    val tileState: StateFlow<TileState> = _tileState.asStateFlow()

    fun loadTiles(
        visibleArea: Rect,
        assetManager: AssetManager
    ) {
        val startX = visibleArea.left.toInt() / tileSize
        val endX = visibleArea.right.toInt() / tileSize
        val startY = visibleArea.top.toInt() / tileSize
        val endY = visibleArea.bottom.toInt() / tileSize

        for (x in startX..endX)
            for (y in startY..endY) {
                val tileBitmap = tileProvider.getTileBitmap(
                    x = x * tileSize,
                    y = y * tileSize,
                    assetManager = assetManager
                ) ?: continue

                _tileState.update { tileState ->
                    val tile = tileState.tiles.firstOrNull { tile ->
                        tile.x == x && tile.y == y
                    }?.copy(bitmap = tileBitmap) ?: Tile(
                        x = x,
                        y = y,
                        bitmap = tileBitmap
                    )

                    tileState.copy(
                        tiles = tileState.tiles.plus(tile).fastDistinctBy { tile ->
                            tile.x to tile.y
                        }
                    )
                }
            }
    }

    fun draw(
        tiles: Collection<Tile>,
        canvas: Canvas
    ) {
        tiles.forEach { tile ->
            canvas.nativeCanvas.drawBitmap(
                tile.bitmap,
                tile.x.toFloat() * tileSize,
                tile.y.toFloat() * tileSize,
                null
            )
        }
    }
}