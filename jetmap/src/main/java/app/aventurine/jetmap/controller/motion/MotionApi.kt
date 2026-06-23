package app.aventurine.jetmap.controller.motion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MotionApi {
    val levelState: StateFlow<Int>
    val motionState: StateFlow<MotionState>
    val visibleAreaFlow: Flow<VisibleArea>
    fun changeLevel(levelUpdateScope: (Int) -> Int)
}