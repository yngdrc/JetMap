package app.aventurine.jetmap.controller.tile

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.descriptor.TileDescriptor
import app.aventurine.jetmap.models.Tile
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TileController(
    parentScope: CoroutineScope,
    val tileProvider: TileProvider,
    val config: JetMapConfig
) : TileApi {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _renderTilesFlow: MutableSharedFlow<TileDescriptor> = MutableSharedFlow()

    private val _tileState: MutableStateFlow<TileState> = MutableStateFlow(emptyList())
    override val tileState: StateFlow<TileState> = _tileState.asStateFlow()

    init {
        scope.launch {
            _renderTilesFlow.mapNotNull(::getTile)
                .collect { tile ->
                    _tileState.update { tileState ->
                        val existingTile = tileState.find {
                            it.x == tile.x && it.y == tile.y && it.z == tile.z
                        }

                        if (existingTile != null)
                            return@update tileState

                        tileState.plus(tile)
                    }
                }
        }
    }

    internal suspend fun onVisibleAreaChanged(
        visibleArea: VisibleArea,
        level: Int
    ) {
        recycleTiles(visibleArea = visibleArea, level = level)
        getTiles(visibleArea = visibleArea, level = level)
    }

    private fun recycleTiles(
        visibleArea: VisibleArea,
        level: Int
    ) {
        val tilesToRecycle = _tileState.value.filter { tile ->
            tile.x !in visibleArea.first || tile.y !in visibleArea.second || tile.z != level
        }.toSet()

        _tileState.update { tiles ->
            tiles.minus(tilesToRecycle)
        }

        tilesToRecycle.forEach { tile -> tile.bitmap.recycle() }
    }

    private suspend fun getTiles(
        visibleArea: VisibleArea,
        level: Int
    ) {
        visibleArea.first.mapNotNull { x ->
            if (x !in 0..<config.xTileCount)
                return@mapNotNull null

            visibleArea.second.mapNotNull { y ->
                if (y !in 0..<config.yTileCount)
                    return@mapNotNull null

                TileDescriptor(x = x, y = y, z = level)
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
                    z = tileDescriptor.z
                )?.use(BitmapFactory::decodeStream)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } ?: return null

        return Tile(
            x = tileDescriptor.x,
            y = tileDescriptor.y,
            z = tileDescriptor.z,
            bitmap = tileBitmap
        )
    }

    internal fun draw(
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