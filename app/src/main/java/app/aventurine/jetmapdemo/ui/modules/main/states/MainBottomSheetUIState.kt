package app.aventurine.jetmapdemo.ui.modules.main.states

import androidx.compose.runtime.Immutable
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor

@Immutable
sealed interface MainBottomSheetUIState {
    data object Initial : MainBottomSheetUIState

    data class MarkerDetails(val markerDescriptor: MarkerDescriptor) : MainBottomSheetUIState

    /**
     * Origin / destination picker, the "Skąd - Dokąd" card of Google Maps.
     * A null [origin] means the user still has to pick the starting point.
     */
    data class RoutePlanning(
        val origin: MarkerDescriptor?,
        val destination: MarkerDescriptor
    ) : MainBottomSheetUIState
}