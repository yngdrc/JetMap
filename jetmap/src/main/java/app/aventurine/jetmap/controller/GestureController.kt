package app.aventurine.jetmap.controller

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class GestureController {
    private val _tapState: MutableStateFlow<Offset?> = MutableStateFlow(null)
    val tapState: StateFlow<Offset?> = _tapState

    private val _focusedMarker: MutableState<Triple<Offset, String, Boolean>?> =
        mutableStateOf(null)
    val focusedMarker: State<Triple<Offset, String, Boolean>?> = _focusedMarker

    fun onTap(
        offset: Offset,
        motionState: MotionState
    ) {
        _tapState.value = offset.div(motionState.zoom).plus(motionState.centroid)
            .rotateBy(-motionState.rotation)
    }

    fun onMarkerFocusChanged(
        offset: Offset,
        existingMarker: Marker?
    ) {
        _focusedMarker.value = Triple(
            offset,
            existingMarker?.description ?: "${offset.x}, ${offset.y}",
            existingMarker != null
        )
    }
}