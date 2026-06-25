package app.aventurine.jetmap.controller.gesture

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class GestureController : GestureApi {
    private val _tapState: MutableStateFlow<Offset?> = MutableStateFlow(null)
    private val _focusedMarker: MutableState<FocusedMarker?> = mutableStateOf(null)

    override val tapState: StateFlow<Offset?> = _tapState
    override val focusedMarker: State<FocusedMarker?> = _focusedMarker

    internal fun onTap(
        offset: Offset,
        motionState: MotionState
    ) {
        _tapState.value = offset.div(motionState.zoom).plus(motionState.centroid)
            .rotateBy(-motionState.rotation)
    }

    internal fun onMarkerFocusChanged(
        tapState: Offset,
        existingMarker: Marker?
    ) {
        _focusedMarker.value = Triple(
            existingMarker?.let { marker ->
                Offset(marker.x.toFloat(), marker.y.toFloat())
            } ?: tapState,
            existingMarker?.description ?: "${tapState.x.toInt()}, ${tapState.y.toInt()}",
            existingMarker != null
        )
    }

    override fun clear() {
        _tapState.update { null }
        _focusedMarker.value = null
    }
}