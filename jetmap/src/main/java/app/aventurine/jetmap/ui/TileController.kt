package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.fastDistinctBy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.SynchronousQueue

class TileController(
    parentScope: CoroutineScope,
    val tileProvider: TileProvider,
    val config: JetMapConfig,
    val assetManager: AssetManager
) {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _renderTilesFlow: MutableSharedFlow<TileDescriptor> = MutableSharedFlow()

    private val _tileState: MutableStateFlow<Collection<Tile>> =
        MutableStateFlow(emptyList())

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

    private suspend fun getTile(
        tileDescriptor: TileDescriptor
    ): Tile? {
        val tileBitmap = withContext(Dispatchers.IO) {
            try {
                tileProvider.getTileInputStream(
                    x = tileDescriptor.x,
                    y = tileDescriptor.y,
                    assetManager = assetManager
                )?.use(BitmapFactory::decodeStream)
            } catch (e: CancellationException) {
                throw e
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