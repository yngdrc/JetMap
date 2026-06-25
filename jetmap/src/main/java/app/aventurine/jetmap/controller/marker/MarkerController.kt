package app.aventurine.jetmap.controller.marker

import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.setBlendMode
import app.aventurine.jetmap.controller.gesture.FocusedMarker
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.descriptor.MarkerDescriptor
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.provider.MarkerProvider
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

internal class MarkerController(
    parentScope: CoroutineScope,
    val markerProvider: MarkerProvider,
    val config: JetMapConfig
) : MarkerApi {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _renderMarkersFlow: MutableSharedFlow<MarkerDescriptor> = MutableSharedFlow()

    private val _markerState: MutableStateFlow<MarkerState> = MutableStateFlow(emptyList())
    override val markerState: StateFlow<MarkerState> = _markerState.asStateFlow()

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

    internal fun draw(
        markers: Collection<Marker>,
        motionState: MotionState,
        focusedMarkerState: FocusedMarker?,
        canvas: Canvas
    ) {
        markers.forEach { marker ->
            val paint = when {
                focusedMarkerState != null -> {
                    if (focusedMarkerState.first.x == marker.x.toFloat()
                        && focusedMarkerState.first.y == marker.y.toFloat()
                    ) {
                        Paint().apply {
                            isFilterBitmap = false
                            colorFilter = PorterDuffColorFilter(
                                Color.White.copy(alpha = 0.2f).toArgb(),
                                PorterDuff.Mode.SRC_ATOP
                            )
                        }
                    } else {
                        null
                    }
                }

                else -> null
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