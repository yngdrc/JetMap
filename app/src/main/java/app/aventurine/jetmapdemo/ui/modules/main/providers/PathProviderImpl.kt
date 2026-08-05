package app.aventurine.jetmapdemo.ui.modules.main.providers

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.domain.models.LadderEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmap.provider.PathSegment
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmapdemo.utils.CostMap
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.BitSet

/**
 * Multi floor routing: A* inside a floor, breadth first search over connected ladders between
 * floors, and a walkable-region index so ladders that are physically unreachable are ignored.
 */
class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher,
    private val ladderRepository: LadderRepository
) : PathProvider {

    /**
     * A connected walkable area. [cells] is indexed relative to the bounding box
     * ([minX], [minY]) - ([maxX], [maxY]) and [width] is the width of that box, so a small room
     * costs a few bytes instead of a floor sized bit set.
     */
    private class Region(
        val floor: Int,
        val cells: BitSet,
        val width: Int,
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int
    ) {
        fun contains(x: Int, y: Int): Boolean =
            x in minX..maxX && y in minY..maxY && cells.get((y - minY) * width + (x - minX))
    }

    // Shared mutable state guarded by a mutex: the previous version mutated a plain HashMap from
    // several coroutines at once.
    private val cacheMutex = Mutex()
    private val costMaps = mutableMapOf<Int, CostMap?>()
    private val regions = mutableListOf<Region>()

    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): List<PathSegment> {
        val (rawStartOffset, startFloor) = startingPoint
        val (rawEndOffset, endFloor) = endingPoint

        return try {
            val startCostMap = costMapOf(startFloor) ?: return emptyList()
            val endCostMap = costMapOf(endFloor) ?: return emptyList()

            // The user may drop a point on a wall or in water; snap it to the closest free cell.
            val startOffset = startCostMap.snap(rawStartOffset) ?: return emptyList()
            val endOffset = endCostMap.snap(rawEndOffset) ?: return emptyList()

            val startRegion = regionOf(point = startOffset, floor = startFloor)
                ?: return emptyList()

            // Same floor and same walkable region: a single A* run is enough.
            if (startFloor == endFloor && startRegion.contains(endOffset.x, endOffset.y)) {
                return singleSegment(
                    level = startFloor,
                    from = startOffset,
                    to = endOffset,
                    costMap = startCostMap
                )
            }

            val endRegion = regionOf(point = endOffset, floor = endFloor) ?: return emptyList()
            val transitions = findLadderRoute(startRegion = startRegion, endRegion = endRegion)
                ?: return emptyList()

            buildSegments(
                startOffset = startOffset,
                startFloor = startFloor,
                endOffset = endOffset,
                endFloor = endFloor,
                endCostMap = endCostMap,
                startCostMap = startCostMap,
                transitions = transitions
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun buildSegments(
        startOffset: IntOffset,
        startFloor: Int,
        endOffset: IntOffset,
        endFloor: Int,
        startCostMap: CostMap,
        endCostMap: CostMap,
        transitions: List<Pair<LadderEntity, LadderEntity>>
    ): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()

        val firstDeparture = transitions.first().first
        segments += segmentOrNull(
            level = startFloor,
            from = startOffset,
            to = IntOffset(firstDeparture.x, firstDeparture.y),
            costMap = startCostMap
        ) ?: return emptyList()

        for (index in 0 until transitions.size - 1) {
            val arrival = transitions[index].second
            val nextDeparture = transitions[index + 1].first
            val costMap = costMapOf(arrival.floor) ?: return emptyList()

            segments += segmentOrNull(
                level = arrival.floor,
                from = IntOffset(arrival.x, arrival.y),
                to = IntOffset(nextDeparture.x, nextDeparture.y),
                costMap = costMap
            ) ?: return emptyList()
        }

        val lastArrival = transitions.last().second
        segments += segmentOrNull(
            level = endFloor,
            from = IntOffset(lastArrival.x, lastArrival.y),
            to = endOffset,
            costMap = endCostMap
        ) ?: return emptyList()

        return segments
    }

    private suspend fun singleSegment(
        level: Int,
        from: IntOffset,
        to: IntOffset,
        costMap: CostMap
    ): List<PathSegment> = segmentOrNull(level, from, to, costMap)?.let(::listOf) ?: emptyList()

    private suspend fun segmentOrNull(
        level: Int,
        from: IntOffset,
        to: IntOffset,
        costMap: CostMap
    ): PathSegment? {
        val points = pathFinder.findPath(start = from, end = to, costMap = costMap)
        return if (points.isEmpty()) null else PathSegment(level = level, points = points)
    }

    /**
     * BFS over ladder connections. Regions are compared by identity, so the same physical area is
     * never flood filled twice.
     */
    private suspend fun findLadderRoute(
        startRegion: Region,
        endRegion: Region
    ): List<Pair<LadderEntity, LadderEntity>>? {
        val visited = mutableSetOf(startRegion)
        val queue = ArrayDeque<Pair<Region, List<Pair<LadderEntity, LadderEntity>>>>()
        queue.add(startRegion to emptyList())

        while (queue.isNotEmpty()) {
            val (region, path) = queue.removeFirst()

            for (ladder in laddersIn(region = region)) {
                val connected = ladderRepository.getConnectedLadder(
                    ladder.x,
                    ladder.y,
                    ladder.floor
                ) ?: continue

                val nextRegion = regionOf(
                    point = IntOffset(connected.x, connected.y),
                    floor = connected.floor
                ) ?: continue

                if (!visited.add(nextRegion)) {
                    continue
                }

                val newPath = path + (ladder to connected)
                if (nextRegion === endRegion) {
                    return newPath
                }

                queue.add(nextRegion to newPath)
            }
        }

        return null
    }

    private suspend fun laddersIn(region: Region): List<LadderEntity> =
        ladderRepository.getLaddersByCoordinates(
            coordinates = JetMapConfig.Coordinates(
                startX = region.minX,
                startY = region.minY,
                endX = region.maxX,
                endY = region.maxY
            ),
            floorId = region.floor
        ).filter { ladder -> region.contains(ladder.x, ladder.y) }

    private suspend fun costMapOf(floor: Int): CostMap? = cacheMutex.withLock {
        if (costMaps.containsKey(floor)) {
            costMaps[floor]
        } else {
            mapStitcher.buildCostMap(floor = floor).also { costMaps[floor] = it }
        }
    }

    /**
     * Returns the walkable region containing the point, reusing an already computed one whenever
     * possible. The previous implementation flood filled the whole floor once per ladder.
     */
    private suspend fun regionOf(point: IntOffset, floor: Int): Region? {
        cacheMutex.withLock {
            regions.firstOrNull { region ->
                region.floor == floor && region.contains(point.x, point.y)
            }
        }?.let { return it }

        val costMap = costMapOf(floor) ?: return null
        val region = computeRegion(costMap = costMap, floor = floor, startX = point.x, startY = point.y)
            ?: return null

        return cacheMutex.withLock {
            regions.firstOrNull { cached ->
                cached.floor == floor && cached.contains(point.x, point.y)
            } ?: region.also {
                if (regions.size >= MAX_CACHED_REGIONS) {
                    regions.removeAt(0)
                }
                regions.add(it)
            }
        }
    }

    /**
     * Flood fills the walkable area around the point and returns it as a region whose bit set
     * covers only its own bounding box, not the whole floor.
     */
    private fun computeRegion(
        costMap: CostMap,
        floor: Int,
        startX: Int,
        startY: Int
    ): Region? {
        if (!costMap.isWalkable(startX, startY)) {
            return null
        }

        val width = costMap.width
        val height = costMap.height
        // Floor sized scratch, needed only while filling: the bounding box is unknown up front.
        val visited = BitSet(width * height)
        val queue = ArrayDeque<Int>()

        var minX = startX
        var minY = startY
        var maxX = startX
        var maxY = startY

        val startIndex = startY * width + startX
        visited.set(startIndex)
        queue.add(startIndex)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentX = current % width
            val currentY = current / width

            for (direction in NEIGHBOUR_DX.indices) {
                val neighbourX = currentX + NEIGHBOUR_DX[direction]
                val neighbourY = currentY + NEIGHBOUR_DY[direction]

                if (neighbourX < 0 || neighbourY < 0 || neighbourX >= width || neighbourY >= height) {
                    continue
                }

                val neighbourIndex = neighbourY * width + neighbourX
                if (visited.get(neighbourIndex) || !costMap.isWalkable(neighbourX, neighbourY)) {
                    continue
                }

                visited.set(neighbourIndex)
                if (neighbourX < minX) minX = neighbourX
                if (neighbourY < minY) minY = neighbourY
                if (neighbourX > maxX) maxX = neighbourX
                if (neighbourY > maxY) maxY = neighbourY
                queue.add(neighbourIndex)
            }
        }

        val boundsWidth = maxX - minX + 1
        val boundsHeight = maxY - minY + 1
        val cells = BitSet(boundsWidth * boundsHeight)

        // Re-index the filled cells relative to the bounding box, so a cached region keeps only
        // its own area instead of a floor sized bit set.
        var index = visited.nextSetBit(minY * width + minX)
        val lastIndex = maxY * width + maxX
        while (index in 0..lastIndex) {
            val cellX = index % width
            val cellY = index / width
            if (cellX in minX..maxX) {
                cells.set((cellY - minY) * boundsWidth + (cellX - minX))
            }
            index = visited.nextSetBit(index + 1)
        }

        return Region(
            floor = floor,
            cells = cells,
            width = boundsWidth,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY
        )
    }

    /** Snaps a user picked point to the closest walkable cell of this floor. */
    private fun CostMap.snap(offset: IntOffset): IntOffset? =
        nearestWalkable(x = offset.x, y = offset.y)
            ?.let { (x, y) -> IntOffset(x = x, y = y) }

    private companion object {
        const val MAX_CACHED_REGIONS = 8
        val NEIGHBOUR_DX = intArrayOf(0, 0, -1, 1, -1, -1, 1, 1)
        val NEIGHBOUR_DY = intArrayOf(-1, 1, 0, 0, -1, 1, -1, 1)
    }
}


