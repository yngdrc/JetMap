package app.aventurine.jetmap.controller.marker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.controller.marker.models.Marker
import app.aventurine.jetmap.provider.MarkerProvider
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MarkerController(
    parentScope: CoroutineScope,
    val markerProvider: MarkerProvider,
    val config: JetMapConfig
) : MarkerApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val _markerStateFlow: MutableStateFlow<MarkerState> =
        MutableStateFlow(value = emptyList())
    override val markerStateFlow: StateFlow<MarkerState> = _markerStateFlow.asStateFlow()

    private val _renderMarkersFlow: MutableSharedFlow<List<MarkerDescriptor>> =
        MutableSharedFlow()

    internal val renderMarkersFlow: SharedFlow<List<MarkerDescriptor>> = _renderMarkersFlow.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )

    init {
        scope.launch {
            renderMarkersFlow.map { markerDescriptors ->
                markerDescriptors.filter { markerDescriptor ->
                    _markerStateFlow.value.none { existingMarker ->
                        existingMarker.id == markerDescriptor.id
                    }
                }.mapNotNull { markerDescriptor ->
                    getMarker(markerDescriptor = markerDescriptor)
                }
            }.collect { markers ->
                _markerStateFlow.update { markerState ->
                    markerState.plus(markers)
                }
            }
        }
    }

    internal suspend fun onVisibleAreaChanged(
        visibleAreaRect: Rect,
        level: Int
    ) {
        recycleMarkers(visibleAreaRect = visibleAreaRect, level = level)
        getMarkers(visibleAreaRect = visibleAreaRect, level = level)
    }

    private fun recycleMarkers(
        visibleAreaRect: Rect,
        level: Int
    ) {
        val markersToRecycle = _markerStateFlow.value.filter { marker ->
            marker.shouldRecycle(visibleAreaRect = visibleAreaRect, level = level)
        }.toSet()

        _markerStateFlow.update { markerState ->
            markerState.minus(elements = markersToRecycle)
        }

        markersToRecycle.forEach { marker -> marker.bitmap.recycle() }
    }

    private suspend fun getMarkers(
        visibleAreaRect: Rect,
        level: Int
    ) {
        val markers = withContext(Dispatchers.IO) {
            markerProvider.getMarkers(
                visibleAreaRect = visibleAreaRect,
                level = level,
            )
        }

        _renderMarkersFlow.emit(value = markers)
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
        level: Int,
        pinBitmap: Bitmap,
        canvas: Canvas,
        showMarkers: Boolean
    ) {
        if (!showMarkers) {
            return
        }

        markers.forEach { marker ->
            var paint: Paint? = null
            if (focusedMarker?.id == marker.id) {
                paint = Paint().apply {
                    isFilterBitmap = false
                    colorFilter = PorterDuffColorFilter(
                        Color.White.copy(alpha = 0.2f).toArgb(),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
            }

            canvas.nativeCanvas.drawBitmap(
                marker.bitmap,
                marker.x.toFloat() - marker.bitmap.width / 2,
                marker.y.toFloat() - marker.bitmap.height / 2,
                paint
            )
        }
    }
}