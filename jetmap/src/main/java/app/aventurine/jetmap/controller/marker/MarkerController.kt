package app.aventurine.jetmap.controller.marker

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.aventurine.jetmap.controller.marker.models.Marker
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.provider.MarkerProvider
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

internal class MarkerController(
    private val markerProvider: MarkerProvider,
    private val config: JetMapConfig
) : MarkerApi {

    private val _markerStateFlow: MutableStateFlow<MarkerState> =
        MutableStateFlow(value = emptyList())
    override val markerStateFlow: StateFlow<MarkerState> = _markerStateFlow.asStateFlow()

    /** LRU cache. Bitmaps are never recycled explicitly, see TileController for the rationale. */
    private val cache =
        object : LinkedHashMap<String, Marker>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Marker>
            ): Boolean = size > CACHE_SIZE
        }

    private val inFlight = mutableSetOf<String>()
    private val cacheMutex = Mutex()
    private val loadSemaphore = Semaphore(permits = PARALLELISM)

    private var visibleAreaRect: Rect = Rect.Zero
    private var level: Int = config.initialLevel

    internal suspend fun onVisibleAreaChanged(
        visibleAreaRect: Rect,
        level: Int
    ) {
        this.visibleAreaRect = visibleAreaRect
        this.level = level

        val descriptors = withContext(Dispatchers.IO) {
            try {
                markerProvider.getMarkers(visibleAreaRect = visibleAreaRect, level = level)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
        }

        publish()
        loadMarkers(descriptors = descriptors)
    }

    private suspend fun loadMarkers(descriptors: List<MarkerDescriptor>) = coroutineScope {
        val toLoad = cacheMutex.withLock {
            descriptors.filter { descriptor ->
                !cache.containsKey(descriptor.id) && inFlight.add(descriptor.id)
            }
        }

        if (toLoad.isEmpty()) {
            return@coroutineScope
        }

        toLoad.forEach { descriptor ->
            launch {
                try {
                    val marker = loadSemaphore.withPermit {
                        getMarker(markerDescriptor = descriptor)
                    }

                    cacheMutex.withLock {
                        inFlight.remove(descriptor.id)
                        if (marker != null) {
                            cache[descriptor.id] = marker
                        }
                    }

                    publish()
                } catch (e: CancellationException) {
                    cacheMutex.withLock { inFlight.remove(descriptor.id) }
                    throw e
                }
            }
        }
    }

    private suspend fun publish() {
        val snapshot = cacheMutex.withLock { cache.values.toList() }
        val currentLevel = level
        val rect = visibleAreaRect

        _markerStateFlow.update {
            snapshot.filter { marker ->
                marker.z == currentLevel &&
                        marker.x.toFloat() in rect.left..rect.right &&
                        marker.y.toFloat() in rect.top..rect.bottom
            }
        }
    }

    private suspend fun getMarker(
        markerDescriptor: MarkerDescriptor
    ): Marker? {
        val markerBitmap = withContext(Dispatchers.IO) {
            try {
                markerProvider.getMarkerInputStream(
                    markerDescriptor = markerDescriptor
                )?.use(BitmapFactory::decodeStream)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } ?: return null

        return Marker(
            x = markerDescriptor.x,
            y = markerDescriptor.y,
            z = markerDescriptor.z,
            bitmap = markerBitmap,
            description = markerDescriptor.description
        )
    }

    internal fun draw(
        markers: Collection<Marker>,
        focusedMarker: MarkerDescriptor?,
        canvas: Canvas,
        showMarkers: Boolean
    ) {
        if (!showMarkers) {
            return
        }

        markers.forEach { marker ->
            if (marker.bitmap.isRecycled) {
                return@forEach
            }

            canvas.nativeCanvas.drawBitmap(
                marker.bitmap,
                marker.x.toFloat() - marker.bitmap.width / 2,
                marker.y.toFloat() - marker.bitmap.height / 2,
                if (focusedMarker?.id == marker.id) focusPaint else null
            )
        }
    }

    /** Allocated once instead of per marker per frame. */
    private val focusPaint: Paint = Paint().apply {
        isFilterBitmap = false
        colorFilter = PorterDuffColorFilter(
            Color.White.copy(alpha = 0.2f).toArgb(),
            PorterDuff.Mode.SRC_ATOP
        )
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val CACHE_SIZE = 512
        const val PARALLELISM = 4
    }
}

