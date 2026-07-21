package app.aventurine.jetmap.provider

import androidx.compose.ui.unit.IntOffset

interface PathProvider {
    suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): Map<Int, List<List<IntOffset>>>
}