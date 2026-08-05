package app.aventurine.jetmap.controller.tile

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.controller.tile.models.Tile
import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

internal class TileController(
    private val tileProvider: TileProvider,
    private val config: JetMapConfig
) : TileApi {

    private val _tileStateFlow: MutableStateFlow<TileState> = MutableStateFlow(value = TileState())
    override val tileStateFlow: StateFlow<TileState> = _tileStateFlow.asStateFlow()

    private val _terrainTypeStateFlow: MutableStateFlow<TerrainType> = MutableStateFlow(
        value = TerrainType.NORMAL
    )
    override val terrainTypeStateFlow: StateFlow<TerrainType> = _terrainTypeStateFlow.asStateFlow()

    /**
     * LRU cache. Bitmaps are never recycled explicitly: a recycled bitmap can still be referenced
     * by the frame currently being drawn, which crashes with "trying to use a recycled bitmap".
     * Dropping the reference and letting the GC collect it is both safe and fast enough.
     */
    private val cache = object : LinkedHashMap<String, Tile>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Tile>): Boolean =
            size > config.tileCacheSize
    }

    private val inFlight = mutableSetOf<String>()
    private val cacheMutex = Mutex()
    private val loadSemaphore = Semaphore(permits = config.tileParallelism.coerceAtLeast(1))

    private var visibleArea: VisibleArea = VisibleArea(0, 0, 0, 0)
    private var level: Int = config.initialLevel
    private var terrainType: TerrainType = TerrainType.NORMAL

    internal suspend fun onVisibleAreaChanged(
        visibleArea: VisibleArea,
        level: Int,
        terrainType: TerrainType
    ) {
        this.visibleArea = visibleArea
        this.level = level
        this.terrainType = terrainType

        // Render whatever is already cached before hitting the disk.
        publish()
        loadTiles(descriptors = buildDescriptors())
    }

    private fun buildDescriptors(): List<TileDescriptor> {
        val descriptors = mutableListOf<TileDescriptor>()

        for (x in visibleArea.left..visibleArea.right) {
            if (x !in 0..<config.xTileCount) continue
            for (y in visibleArea.top..visibleArea.bottom) {
                if (y !in 0..<config.yTileCount) continue
                descriptors.add(
                    TileDescriptor(x = x, y = y, z = level, terrainType = terrainType)
                )
            }
        }

        return descriptors
    }

    /**
     * Loads the missing tiles in parallel and publishes progressively, so the map fills in at once
     * instead of tile by tile.
     */
    private suspend fun loadTiles(descriptors: List<TileDescriptor>) = coroutineScope {
        val toLoad = cacheMutex.withLock {
            descriptors.filter { descriptor ->
                !cache.containsKey(descriptor.key) && inFlight.add(descriptor.key)
            }
        }

        if (toLoad.isEmpty()) {
            return@coroutineScope
        }

        toLoad.forEach { descriptor ->
            launch {
                try {
                    val tile = loadSemaphore.withPermit { getTile(tileDescriptor = descriptor) }
                    cacheMutex.withLock {
                        inFlight.remove(descriptor.key)
                        if (tile != null) {
                            cache[descriptor.key] = tile
                        }
                    }
                    publish()
                } catch (e: CancellationException) {
                    cacheMutex.withLock { inFlight.remove(descriptor.key) }
                    throw e
                }
            }
        }
    }

    private suspend fun publish() {
        val snapshot = cacheMutex.withLock { cache.values.toList() }

        val currentLevel = level
        val currentTerrain = terrainType

        val tiles = snapshot.filter { tile ->
            tile.z == currentLevel && tile.terrainType == currentTerrain && tile.isVisible()
        }

        val coveredPositions = tiles.mapTo(HashSet()) { it.positionKey }

        // Nearest already decoded tile of another level for the same position.
        val fallbackTiles = snapshot
            .filter { tile -> tile.isVisible() && tile.positionKey !in coveredPositions }
            .groupBy { it.positionKey }
            .mapNotNull { (_, candidates) ->
                candidates.minByOrNull { candidate ->
                    abs(candidate.z - currentLevel) * 10 +
                            if (candidate.terrainType == currentTerrain) 0 else 1
                }
            }

        _tileStateFlow.update {
            TileState(tiles = tiles, fallbackTiles = fallbackTiles)
        }
    }

    private fun Tile.isVisible(): Boolean =
        x in visibleArea.left..visibleArea.right && y in visibleArea.top..visibleArea.bottom

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
            bitmap = tileBitmap,
            terrainType = tileDescriptor.terrainType
        )
    }

    internal fun draw(
        tileState: TileState,
        canvas: Canvas
    ) {
        tileState.fallbackTiles.forEach { tile ->
            drawTile(tile = tile, canvas = canvas, paint = fallbackPaint)
        }

        tileState.tiles.forEach { tile ->
            drawTile(tile = tile, canvas = canvas, paint = null)
        }
    }

    private fun drawTile(tile: Tile, canvas: Canvas, paint: Paint?) {
        if (tile.bitmap.isRecycled) {
            return
        }

        canvas.nativeCanvas.drawBitmap(
            tile.bitmap,
            tile.x.toFloat() * config.tileSize,
            tile.y.toFloat() * config.tileSize,
            paint
        )
    }

    override fun toggleTerrainType() {
        _terrainTypeStateFlow.update { currentState ->
            when (currentState) {
                TerrainType.NORMAL -> TerrainType.WAYPOINT_COST
                else -> TerrainType.NORMAL
            }
        }
    }

    /** Allocated once: creating a Paint per frame is guaranteed GC churn in the render loop. */
    private val fallbackPaint: Paint = Paint().apply {
        isFilterBitmap = false
        alpha = FALLBACK_ALPHA
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val FALLBACK_ALPHA = 110
    }
}

