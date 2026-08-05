package app.aventurine.jetmap.controller.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MotionApi {
    val levelStateFlow: StateFlow<Int>
    val motionStateFlow: StateFlow<MotionState>
    val visibleAreaFlow: Flow<VisibleArea>
    val cameraModeFlow: StateFlow<CameraMode>
    val canvasSizeFlow: StateFlow<IntSize>

    fun changeLevel(level: Int)

    fun moveTo(
        offset: Offset,
        level: Int,
        zoom: Float? = null,
        rotation: Float? = null,
        animate: Boolean = true
    )

    /**
     * Frames [bounds] (map units) inside the visible viewport, honouring the given screen padding.
     */
    fun fitBounds(
        bounds: Rect,
        level: Int,
        paddingLeft: Float = 0f,
        paddingTop: Float = 0f,
        paddingRight: Float = 0f,
        paddingBottom: Float = 0f,
        animate: Boolean = true
    )

    fun setCameraMode(cameraMode: CameraMode)
}