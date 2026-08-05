package app.aventurine.jetmap.controller.gesture

import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.gesture.models.MapTapResult
import app.aventurine.jetmap.controller.gesture.models.TapEvent
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.utils.screenToMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class GestureController(
    private val markerProvider: MarkerProvider
) : GestureApi {

    /**
     * Buffered so a tap emitted before the collector attaches is not silently dropped.
     */
    private val _tapFlow: MutableSharedFlow<TapEvent> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )

    internal val tapFlow: SharedFlow<TapEvent> = _tapFlow.asSharedFlow()

    private val _tapResultFlow: MutableSharedFlow<MapTapResult> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )

    override val tapResultFlow: SharedFlow<MapTapResult> = _tapResultFlow.asSharedFlow()

    private val _focusedMarkerFlow: MutableStateFlow<MarkerDescriptor?> =
        MutableStateFlow(value = null)
    override val focusedMarkerFlow: StateFlow<MarkerDescriptor?> = _focusedMarkerFlow.asStateFlow()

    internal fun onTap(
        tapArea: Float,
        offset: Offset,
        motionState: MotionState,
        level: Int
    ) {
        _tapFlow.tryEmit(
            value = TapEvent(
                mapOffset = motionState.screenToMap(screenOffset = offset),
                level = level,
                tapArea = tapArea
            )
        )
    }

    internal suspend fun onMarkerFocusChanged(tapEvent: TapEvent) {
        val markerDescriptor = try {
            markerProvider.getMarker(
                x = tapEvent.mapOffset.x.toInt(),
                y = tapEvent.mapOffset.y.toInt(),
                z = tapEvent.level,
                tapArea = tapEvent.tapArea
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        changeFocusedMarker(focusedMarker = markerDescriptor)

        _tapResultFlow.emit(
            value = MapTapResult(
                mapOffset = tapEvent.mapOffset,
                level = tapEvent.level,
                marker = markerDescriptor
            )
        )
    }

    override fun changeFocusedMarker(focusedMarker: MarkerDescriptor?) {
        _focusedMarkerFlow.update { focusedMarker }
    }
}
