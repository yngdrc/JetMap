package app.aventurine.jetmap.controller.ui

import androidx.compose.runtime.State

interface UIApi {
    val uiState: State<UIState>

    fun toggleMarkers()
}