package app.aventurine.jetmap.controller.gesture

import androidx.compose.runtime.State
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import kotlinx.coroutines.flow.StateFlow

interface GestureApi {
    val focusedMarkerFlow: StateFlow<MarkerDescriptor?>
    fun changeFocusedMarker(focusedMarker: MarkerDescriptor?)
}