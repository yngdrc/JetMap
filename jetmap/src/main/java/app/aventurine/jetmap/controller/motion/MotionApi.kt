package app.aventurine.jetmap.controller.motion

import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MotionApi {
    val levelStateFlow: StateFlow<Int>
    val motionStateFlow: StateFlow<MotionState>
    val visibleAreaFlow: Flow<VisibleArea>
    fun changeLevel(level: Int)
    fun moveTo(offset: Offset, level: Int, zoom: Float? = null)
}