package app.aventurine.jetmap.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.scale
import app.aventurine.jetmap.descriptor.MarkerDescriptor
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.rotateBy
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
import kotlin.collections.forEach

class MarkerController(
    parentScope: CoroutineScope,
    val markerProvider: MarkerProvider,
    val config: JetMapConfig
) {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _renderMarkersFlow: MutableSharedFlow<MarkerDescriptor> = MutableSharedFlow()

    private val _markerState: MutableStateFlow<Collection<Marker>> =
        MutableStateFlow(emptyList())

    val markerState: StateFlow<Collection<Marker>> = _markerState.asStateFlow()

    init {
        scope.launch {
            _renderMarkersFlow.mapNotNull(::getMarker)
                .collect { marker ->
                    _markerState.update { markerState ->
                        val existingMarker = markerState.find {
                            it.x == marker.x && it.y == marker.y && it.z == marker.z
                        }

                        if (existingMarker != null)
                            return@update markerState

                        markerState.plus(marker)
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
        val markersToRecycle = _markerState.value.filter { marker ->
            marker.x.toFloat() !in (visibleAreaRect.left..visibleAreaRect.right)
                    || marker.y.toFloat() !in (visibleAreaRect.top..visibleAreaRect.bottom)
                    || marker.z != level
        }.toSet()

        _markerState.update { markers ->
            markers.minus(markersToRecycle)
        }

        markersToRecycle.forEach { marker -> marker.bitmap.recycle() }
    }

    private suspend fun getMarkers(
        visibleAreaRect: Rect,
        level: Int
    ) {
        val markers = markerProvider.getMarkers(visibleAreaRect = visibleAreaRect, level = level)
        markers.forEach { marker ->
            _markerState.update { markerState ->
                val existingMarker = markerState.find {
                    it.x == marker.x && it.y == marker.y && it.z == marker.z
                }

                if (existingMarker != null)
                    return@update markerState

                markerState.plus(marker)
            }
        }
    }

    private suspend fun getMarker(
        markerDescriptor: MarkerDescriptor
    ): Marker? = withContext(Dispatchers.IO) {
        try {
            markerProvider.getMarker(
                x = markerDescriptor.x,
                y = markerDescriptor.y,
                z = markerDescriptor.z
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext null
        }
    }

    fun draw(
        markers: Collection<Marker>,
        canvas: Canvas
    ) {
        markers.forEach { marker ->
            canvas.nativeCanvas.drawBitmap(
                marker.bitmap.scale(
                    width = 8,
                    height = 8,
                    filter = false
                ),
                marker.x.toFloat() - 4,
                marker.y.toFloat() - 4,
                null
            )
        }
    }
}