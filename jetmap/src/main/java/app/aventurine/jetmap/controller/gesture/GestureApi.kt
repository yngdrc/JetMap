package app.aventurine.jetmap.controller.gesture

import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.StateFlow

interface GestureApi {
    val tapState: StateFlow<Offset?>
    val focusedMarker: State<FocusedMarker?>
    fun clear()
}