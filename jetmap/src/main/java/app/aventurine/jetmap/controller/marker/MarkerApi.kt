package app.aventurine.jetmap.controller.marker

import kotlinx.coroutines.flow.StateFlow

interface MarkerApi {
    val markerStateFlow: StateFlow<MarkerState>
}