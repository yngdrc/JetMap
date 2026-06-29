package app.aventurine.jetmap.controller.gesture

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.motion.MotionState
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.utils.rotateBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class GestureController(
    parentScope: CoroutineScope,
    private val markerProvider: MarkerProvider
) : GestureApi {
    private val scope: CoroutineScope = CoroutineScope(
        context = parentScope.coroutineContext + SupervisorJob()
    )

    private val _tapFlow: MutableSharedFlow<Pair<Offset, Int>?> = MutableStateFlow(value = null)
    internal val tapFlow: SharedFlow<Pair<Offset, Int>?> = _tapFlow.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )

    private val _focusedMarkerFlow: MutableStateFlow<MarkerDescriptor?> =
        MutableStateFlow(value = null)
    override val focusedMarkerFlow: StateFlow<MarkerDescriptor?> = _focusedMarkerFlow.asStateFlow()

    internal fun onTap(
        offset: Offset,
        motionState: MotionState,
        level: Int
    ) {
        scope.launch {
            _tapFlow.emit(
                value = offset.div(operand = motionState.zoom).plus(other = motionState.centroid)
                    .rotateBy(angle = -motionState.rotation) to level
            )
        }
    }

    internal suspend fun onMarkerFocusChanged(
        offset: Offset,
        level: Int
    ) {
        val markerDescriptor = markerProvider.getMarker(
            x = offset.x.toInt(),
            y = offset.y.toInt(),
            z = level
        )

        changeFocusedMarker(focusedMarker = markerDescriptor)
    }

    override fun changeFocusedMarker(focusedMarker: MarkerDescriptor?) {
        _focusedMarkerFlow.update { focusedMarker }
    }
}