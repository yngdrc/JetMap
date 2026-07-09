package app.aventurine.jetmap.controller.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

internal class UIController : UIApi {
    private val _uiState: MutableState<UIState> = mutableStateOf(
        value = UIState(showMarkers = false)
    )

    override val uiState: State<UIState> = _uiState

    override fun toggleMarkers() {
        _uiState.value = _uiState.value.copy(showMarkers = !_uiState.value.showMarkers)
    }
}