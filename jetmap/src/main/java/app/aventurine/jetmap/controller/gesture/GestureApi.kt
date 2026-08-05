package app.aventurine.jetmap.controller.gesture

import app.aventurine.jetmap.controller.gesture.models.MapTapResult
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GestureApi {
    val focusedMarkerFlow: StateFlow<MarkerDescriptor?>

    /**
     * Every tap on the map, already resolved against the marker provider.
     * Emitted for empty space too, which is what makes "pick any point as route origin" possible.
     */
    val tapResultFlow: SharedFlow<MapTapResult>

    fun changeFocusedMarker(focusedMarker: MarkerDescriptor?)
}
