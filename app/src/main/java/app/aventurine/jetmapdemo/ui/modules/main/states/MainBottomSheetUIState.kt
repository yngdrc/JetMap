package app.aventurine.jetmapdemo.ui.modules.main.states

import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor

sealed class MainBottomSheetUIState {
    object Initial : MainBottomSheetUIState()
    data class MarkerDetails(val markerDescriptor: MarkerDescriptor) : MainBottomSheetUIState()
    data class Navigation(
        val startMarkerDescriptor: MarkerDescriptor?,
        val endMarkerDescriptor: MarkerDescriptor
    ) : MainBottomSheetUIState()
}