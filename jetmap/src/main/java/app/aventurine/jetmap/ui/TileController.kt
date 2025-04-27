package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class TileController(
    parentScope: CoroutineScope,
    val tileProvider: TileProvider,
    val config: JetMapConfig,
    val assetManager: AssetManager
) {
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(parentScope.coroutineContext + singleThreadDispatcher)

    private val _renderTilesFlow: MutableSharedFlow<TileDescriptor> = MutableSharedFlow()

    private val _tileState: MutableStateFlow<Collection<Tile>> = MutableStateFlow(emptyList())
    val tileState: StateFlow<Collection<Tile>> = _tileState.asStateFlow()

    init {
        scope.launch {
            _renderTilesFlow
                .map(::getTile)
                .filterNotNull()
                .collect { tile ->
                    _tileState.update { tileState ->
                        val existingTile = tileState.find { it.x == tile.x && it.y == tile.y }
                        if (existingTile != null)
                            return@update tileState

                        tileState.plus(tile)
                    }
                }
        }
    }

    internal suspend fun onVisibleAreaChanged(
        visibleArea: VisibleArea
    ) {
        recycleTiles(visibleArea = visibleArea)
        getTiles(visibleArea = visibleArea)
    }

    private fun recycleTiles(
        visibleArea: VisibleArea
    ) {
        val tilesToRecycle = _tileState.value.filter { tile ->
            tile.x !in visibleArea.first || tile.y !in visibleArea.second
        }

        _tileState.update { tiles ->
            tiles.minus(tilesToRecycle)
        }

        tilesToRecycle.forEach { tile -> tile.bitmap.recycle() }
    }

    private suspend fun getTiles(
        visibleArea: VisibleArea
    ) {
        visibleArea.first.mapNotNull { x ->
            if (x !in 0..config.xTileCount - 1)
                return@mapNotNull null

            visibleArea.second.mapNotNull { y ->
                if (y !in 0..config.yTileCount - 1)
                    return@mapNotNull null

                TileDescriptor(x = x, y = y)
            }
        }.flatten().forEach { tileDescriptor ->
            _renderTilesFlow.emit(tileDescriptor)
        }
    }

    private fun getTile(
        tileDescriptor: TileDescriptor
    ): Tile? {
        val tileBitmap = tileProvider.getTileInputStream(
            x = tileDescriptor.x * config.tileSize,
            y = tileDescriptor.y * config.tileSize,
            assetManager = assetManager
        )?.use { inputStream ->
            try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        } ?: return null

        return Tile(x = tileDescriptor.x, y = tileDescriptor.y, bitmap = tileBitmap)
    }

    fun draw(
        tiles: Collection<Tile>,
        canvas: Canvas
    ) {
        tiles.forEach { tile ->
            canvas.nativeCanvas.drawBitmap(
                tile.bitmap,
                tile.x.toFloat() * config.tileSize,
                tile.y.toFloat() * config.tileSize,
                null
            )
        }
    }
}