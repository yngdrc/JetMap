package app.aventurine.jetmapdemo.ui.modules.main.providers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import app.aventurine.jetmapdemo.utils.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.core.graphics.get
import app.aventurine.jetmap.domain.models.LadderEntity
import app.aventurine.jetmap.ui.JetMapConfig

class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher,
    private val ladderRepository: LadderRepository
) : PathProvider {
    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): Map<Int, List<IntOffset>> {
        val (startOffset, startFloor) = startingPoint
        val (endOffset, endFloor) = endingPoint

        val (startLadders, endLadders) = coroutineScope {
            val startLadders = async { getLaddersInRegion(point = startOffset, floor = startFloor) }
            val endLadders = async { getLaddersInRegion(point = endOffset, floor = endFloor) }

            startLadders.await() to endLadders.await()
        }

        if (startLadders == endLadders) {
            val mapBitmap = withContext(Dispatchers.IO) {
                mapStitcher.stitch(floor = startFloor)
            }

            if (mapBitmap == null) {
                return mapOf()
            }

            val path = pathFinder.aStar(
                start = Node(
                    offset = startOffset,
                    g = 0,
                    h = 0,
                    parent = null
                ),
                end = endOffset,
                mapBitmap = mapBitmap
            )

            return mapOf(startFloor to path)
        }

        val startSubLadders = coroutineScope {
            val visited = HashSet<LadderEntity>()
            val l = mutableListOf<Collection<LadderEntity>>()

            startLadders.mapNotNull { ladder ->
                val connectedLadder = ladderRepository.getConnectedLadder(
                    x = ladder.x,
                    y = ladder.y,
                    floor = ladder.floor
                ) ?: return@mapNotNull null

                if (visited.contains(connectedLadder)) {
                    return@mapNotNull null
                }

                visited.add(connectedLadder)
                val ladders = getLaddersInRegion(
                    point = IntOffset(
                        x = connectedLadder.x,
                        y = connectedLadder.y
                    ),
                    floor = connectedLadder.floor
                )

                l.add(ladders)
            }

            l.distinct()
        }

        val endSubLadders = coroutineScope {
            val visited = HashSet<LadderEntity>()
            val l = mutableListOf<Collection<LadderEntity>>()

            endLadders.forEach { ladder ->
                val connectedLadder = ladderRepository.getConnectedLadder(
                    x = ladder.x,
                    y = ladder.y,
                    floor = ladder.floor
                ) ?: return@forEach

                if (visited.contains(connectedLadder)) {
                    return@forEach
                }

                visited.add(connectedLadder)
                val ladders = getLaddersInRegion(
                    point = IntOffset(
                        x = connectedLadder.x,
                        y = connectedLadder.y
                    ),
                    floor = connectedLadder.floor
                )

                l.add(ladders)
            }

            l.distinct()
        }

        val i = listOf(startLadders)
            .plus(startSubLadders)
            .intersect(
                listOf(endLadders)
                    .plus(endSubLadders)
                    .toSet()
            ).toList()

        val result = i.fold(mutableMapOf<IntOffset, List<LadderEntity>>()) { current, next ->
            val grouped = next.groupBy { IntOffset(it.x, it.y) }
            grouped.forEach { (offset, entities) ->
                current[offset] = current[offset]?.plus(entities) ?: entities
            }

            current
        }.filter { entry ->
            entry.value.size > 1
        }.values.map { ladders ->
            listOf(
                startingPoint,
                endingPoint
            ).mapNotNull { (offset, floor) ->
                val destination = ladders.firstOrNull { ladder ->
                    ladder.floor == floor
                } ?: return@mapNotNull null

                val mapBitmap = withContext(Dispatchers.IO) {
                    mapStitcher.stitch(floor = floor)
                }

                if (mapBitmap == null) {
                    return mapOf()
                }

                val path = pathFinder.aStar(
                    start = Node(
                        offset = offset,
                        g = 0,
                        h = 0,
                        parent = null
                    ),
                    end = IntOffset(destination.x, destination.y),
                    mapBitmap = mapBitmap
                )

                mapBitmap.recycle()
                floor to path
            }
        }.flatten().toMap()

        return result
    }

    private fun Bitmap.getRegionAt(x: Int, y: Int): Set<IntOffset> {
        val startColor = this[x, y]
        val startR = Color.red(startColor)
        val startG = Color.green(startColor)
        val startB = Color.blue(startColor)
        val normalizedStart = Color.rgb(startR, startG, startB)

        if (normalizedStart == Color.rgb(255, 255, 0)) return emptySet()

        val visited = HashSet<IntOffset>()
        val queue = ArrayDeque<IntOffset>()
        val start = IntOffset(x, y)

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val neighbors = listOf(
                IntOffset(current.x, current.y - 1),
                IntOffset(current.x, current.y + 1),
                IntOffset(current.x - 1, current.y),
                IntOffset(current.x + 1, current.y),
                IntOffset(current.x - 1, current.y - 1),
                IntOffset(current.x - 1, current.y + 1),
                IntOffset(current.x + 1, current.y - 1),
                IntOffset(current.x + 1, current.y + 1),
            )

            for (neighbor in neighbors) {
                if (neighbor in visited) continue
                if (neighbor.x < 0 || neighbor.y < 0 || neighbor.x >= width || neighbor.y >= height) continue

                val pixel = this[neighbor.x, neighbor.y]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val color = Color.rgb(r, g, b)

                if (color != Color.rgb(255, 255, 0)) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        return visited
    }

    private suspend fun getLaddersInRegion(
        point: IntOffset,
        floor: Int
    ): Collection<LadderEntity> = try {
        val mapBitmap = withContext(Dispatchers.IO) {
            mapStitcher.stitch(floor = floor)
        }

        if (mapBitmap == null) {
            return listOf()
        }

        val region = mapBitmap.getRegionAt(x = point.x, y = point.y)

        val startX = region.minOf { region -> region.x }
        val startY = region.minOf { region -> region.y }
        val endX = region.maxOf { region -> region.x }
        val endY = region.maxOf { region -> region.y }

//        val regionBitmap = createBitmap(endX - startX, endY - startY)
//        val regionCanvas = Canvas(regionBitmap).apply {
//            region.forEach { r ->
//                drawPoint(
//                    endX - r.x.toFloat(),
//                    endY - r.y.toFloat(),
//                    Paint().apply {
//                        isAntiAlias = false
//                        isFilterBitmap = false
//                        color = Color.RED
//                    }
//                )
//            }
//        }
//
//        regionBitmap.recycle()
        val laddersInRegion = withContext(Dispatchers.IO) {
            ladderRepository.getLaddersByCoordinates(
                coordinates = JetMapConfig.Coordinates(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY
                ),
                floorId = floor
            ).filter { ladder ->
                region.contains(IntOffset(x = ladder.x, y = ladder.y))
            }
        }

        mapBitmap.recycle()
        return laddersInRegion
    } catch (e: Exception) {
        listOf()
    }
}