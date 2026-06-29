package app.aventurine.jetmap.controller.tile

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import app.aventurine.jetmap.controller.tile.models.Tile
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.shouldRecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TileController(
    parentScope: CoroutineScope,
    val tileProvider: TileProvider,
    val config: JetMapConfig
) : TileApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val _tileStateFlow: MutableStateFlow<TileState> = MutableStateFlow(value = emptyList())
    override val tileStateFlow: StateFlow<TileState> = _tileStateFlow.asStateFlow()

    private val _terrainTypeStateFlow: MutableStateFlow<TerrainType> = MutableStateFlow(
        value = TerrainType.NORMAL
    )

    override val terrainTypeStateFlow: StateFlow<TerrainType> = _terrainTypeStateFlow.asStateFlow()

    private val _renderTilesFlow: MutableSharedFlow<TileDescriptor> = MutableSharedFlow()
    internal val renderTilesFlow: SharedFlow<TileDescriptor> = _renderTilesFlow.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )

    init {
        scope.launch {
            renderTilesFlow.filter { tileDescriptor ->
                _tileStateFlow.value.none { existingTile ->
                    existingTile.id == tileDescriptor.id
                }
            }.mapNotNull(::getTile)
                .collect { tile ->
                    _tileStateFlow.update { tileState ->
                        tileState.plus(tile)
                    }
                }
        }
    }

    internal suspend fun onVisibleAreaChanged(
        visibleArea: VisibleArea,
        level: Int,
        terrainType: TerrainType
    ) {
        recycleTiles(visibleArea = visibleArea, level = level)
        getTiles(visibleArea = visibleArea, level = level, terrainType = terrainType)
    }

    private fun recycleTiles(
        visibleArea: VisibleArea,
        level: Int
    ) {
        val tilesToRecycle = _tileStateFlow.value.filter { tile ->
            tile.shouldRecycle(visibleArea = visibleArea, level = level)
        }.toSet()

        _tileStateFlow.update { tiles ->
            tiles.minus(elements = tilesToRecycle)
        }

        tilesToRecycle.forEach { tile -> tile.bitmap.recycle() }
    }

    private suspend fun getTiles(
        visibleArea: VisibleArea,
        level: Int,
        terrainType: TerrainType
    ) {
        val xs = (visibleArea.left..visibleArea.right).filter { x ->
            x in 0..<config.xTileCount
        }

        val ys = (visibleArea.top..visibleArea.bottom).filter { y ->
            y in 0..<config.yTileCount
        }

        xs.associateWith { ys }.entries.forEach { (x, ys) ->
            ys.forEach { y ->
                val tileDescriptor = TileDescriptor(
                    x = x,
                    y = y,
                    z = level,
                    terrainType = terrainType
                )

                _renderTilesFlow.emit(value = tileDescriptor)
            }
        }
    }

    private suspend fun getTile(
        tileDescriptor: TileDescriptor
    ): Tile? {
        val tileBitmap = withContext(Dispatchers.IO) {
            try {
                tileProvider.getTileInputStream(
                    tileDescriptor = tileDescriptor
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