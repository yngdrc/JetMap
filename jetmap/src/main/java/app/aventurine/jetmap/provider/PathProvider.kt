package app.aventurine.jetmap.provider

import androidx.compose.ui.unit.IntOffset

/**
 * One continuous, already simplified polyline on a single level.
 * Segments are returned in travel order, so level changes can be turned into maneuvers.
 */
data class PathSegment(
    val level: Int,
    val points: List<IntOffset>
)

interface PathProvider {
    suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): List<PathSegment>
}

