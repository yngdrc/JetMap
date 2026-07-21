package app.aventurine.jetmapdemo.ui.modules.main.providers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.domain.models.LadderEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import app.aventurine.jetmapdemo.utils.Node
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.BitSet

class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher,
    private val ladderRepository: LadderRepository
) : PathProvider {

    companion object {
        private val NEIGHBOR_DX = intArrayOf(0, 0, -1, 1, -1, -1, 1, 1)
        private val NEIGHBOR_DY = intArrayOf(-1, 1, 0, 0, -1, 1, -1, 1)
    }

    private data class RegionResult(
        val bits: BitSet,
        val bitmapWidth: Int,
        val minX: Int, val minY: Int,
        val maxX: Int, val maxY: Int
    ) {
        fun contains(x: Int, y: Int): Boolean = bits.get(y * bitmapWidth + x)
    }

    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): Map<Int, List<List<IntOffset>>> {
        val (startOffset, startFloor) = startingPoint
        val (endOffset, endFloor) = endingPoint

        val bitmapCache = mutableMapOf<Int, Bitmap?>()
        withContext(Dispatchers.IO) {
            setOf(startFloor, endFloor).forEach { floor ->
                bitmapCache[floor] = mapStitcher.stitch(floor)
            }
        }

        try {
            val (startLadders, endLadders) = coroutineScope {
                val startAsync = async { getLaddersInRegion(startOffset, startFloor, bitmapCache) }
                val endAsync = async { getLaddersInRegion(endOffset, endFloor, bitmapCache) }
                startAsync.await() to endAsync.await()
            }

            if (startLadders.toSet() == endLadders.toSet()) {
                val bitmap = bitmapCache[startFloor] ?: return emptyMap()
                return mapOf(
                    startFloor to listOf(
                        pathFinder.aStar(
                            start = Node(offset = startOffset, g = 0, h = 0, parent = null),
                            end = endOffset,
                            mapBitmap = bitmap
                        )
                    )
                )
            }

            val route = findRoute(startLadders, endLadders, bitmapCache) ?: return emptyMap()

            val result = mutableMapOf<Int, MutableList<List<IntOffset>>>()

            val (firstDeparture, _) = route.first()
            bitmapCache[startFloor]?.let { bitmap ->
                result.getOrPut(startFloor) { mutableListOf() }.add(
                    pathFinder.aStar(
                        start = Node(offset = startOffset, g = 0, h = 0, parent = null),
                        end = IntOffset(firstDeparture.x, firstDeparture.y),
                        mapBitmap = bitmap
                    )
                )
            }

            for (i in 0 until route.size - 1) {
                val (_, arrival) = route[i]
                val (nextDeparture, _) = route[i + 1]
                bitmapCache[arrival.floor]?.let { bitmap ->
                    result.getOrPut(arrival.floor) { mutableListOf() }.add(
                        pathFinder.aStar(
                            start = Node(offset = IntOffset(arrival.x, arrival.y), g = 0, h = 0, parent = null),
                            end = IntOffset(nextDeparture.x, nextDeparture.y),
                            mapBitmap = bitmap
                        )
                    )
                }
            }

            val (_, lastArrival) = route.last()
            bitmapCache[endFloor]?.let { bitmap ->
                result.getOrPut(endFloor) { mutableListOf() }.add(
                    pathFinder.aStar(
                        start = Node(offset = IntOffset(lastArrival.x, lastArrival.y), g = 0, h = 0, parent = null),
                        end = endOffset,
                        mapBitmap = bitmap
                    )
                )
            }

            return result
        } finally {
            bitmapCache.values.forEach { it?.recycle() }
        }
    }

    private suspend fun findRoute(
        startLadders: Collection<LadderEntity>,
        endLadders: Collection<LadderEntity>,
        bitmapCache: MutableMap<Int, Bitmap?>
    ): List<Pair<LadderEntity, LadderEntity>>? {
        val endSet = endLadders.toSet()

        val regionCache = mutableMapOf<Triple<Int, Int, Int>, Collection<LadderEntity>>()

        suspend fun regionOf(entry: LadderEntity): Collection<LadderEntity> {
            val key = Triple(entry.x, entry.y, entry.floor)
            return regionCache.getOrPut(key) {
                if (entry.floor !in bitmapCache) {
                    bitmapCache[entry.floor] = withContext(Dispatchers.IO) {
                        mapStitcher.stitch(entry.floor)
                    }
                }
                getLaddersInRegion(IntOffset(entry.x, entry.y), entry.floor, bitmapCache)
            }
        }

        data class State(
            val ladders: Collection<LadderEntity>,
            val path: List<Pair<LadderEntity, LadderEntity>>
        )

        val queue = ArrayDeque<State>()
        val visited = mutableSetOf(startLadders.toSet())
        queue.add(State(startLadders, emptyList()))

        while (queue.isNotEmpty()) {
            val (currentLadders, path) = queue.removeFirst()

            for (ladder in currentLadders) {
                val connected = ladderRepository.getConnectedLadder(ladder.x, ladder.y, ladder.floor)
                    ?: continue

                val nextLadders = regionOf(connected)
                val nextSet = nextLadders.toSet()

                if (nextSet in visited) continue
                visited.add(nextSet)

                val newPath = path + (ladder to connected)

                if (nextSet == endSet) return newPath

                queue.add(State(nextLadders, newPath))
            }
        }

        return null
    }

    private fun Int.isWalkable(): Boolean =
        Color.red(this) != 255 || Color.green(this) != 255 || Color.blue(this) != 0

    private fun Bitmap.computeRegion(startX: Int, startY: Int): RegionResult? {
        if (!this[startX, startY].isWalkable()) return null

        val bits = BitSet(width * height)
        val queue = ArrayDeque<Int>()

        var minX = startX; var minY = startY
        var maxX = startX; var maxY = startY

        bits.set(startY * width + startX)
        queue.add(startY * width + startX)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val cx = current % width
            val cy = current / width

            for (i in NEIGHBOR_DX.indices) {
                val nx = cx + NEIGHBOR_DX[i]
                val ny = cy + NEIGHBOR_DY[i]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                val ni = ny * width + nx
                if (bits.get(ni)) continue
                if (this[nx, ny].isWalkable()) {
                    bits.set(ni)
                    if (nx < minX) minX = nx
                    if (ny < minY) minY = ny
                    if (nx > maxX) maxX = nx
                    if (ny > maxY) maxY = ny
                    queue.add(ni)
                }
            }
        }

        return RegionResult(bits, width, minX, minY, maxX, maxY)
    }

    private suspend fun getLaddersInRegion(
        point: IntOffset,
        floor: Int,
        bitmapCache: Map<Int, Bitmap?>
    ): Collection<LadderEntity> = try {
        val bitmap = bitmapCache[floor] ?: return listOf()
        val region = bitmap.computeRegion(point.x, point.y) ?: return listOf()

        withContext(Dispatchers.IO) {
            ladderRepository.getLaddersByCoordinates(
                coordinates = JetMapConfig.Coordinates(
                    startX = region.minX, startY = region.minY,
                    endX = region.maxX, endY = region.maxY
                ),
                floorId = floor
            ).filter { ladder -> region.contains(ladder.x, ladder.y) }
        }
    } catch (_: Exception) {
        listOf()
    }
}
