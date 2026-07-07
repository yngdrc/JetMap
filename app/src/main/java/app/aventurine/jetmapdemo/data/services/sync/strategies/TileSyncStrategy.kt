package app.aventurine.jetmapdemo.data.services.sync.strategies

import app.aventurine.jetmap.controller.tile.TerrainType
import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmapdemo.data.models.tile.GetTileRequest
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import retrofit2.HttpException

class TileSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val apiService: JetMapApiService,
    private val fileStorage: FileStorage
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            val mapConfig = dataStoreManager.getOrNull()
                ?: return Result.failure(exception = RuntimeException("Map config not found"))

            if (mapConfig !is MapConfigLocalEntity.Default) {
                return Result.failure(exception = RuntimeException("Map config is empty"))
            }

            for (x in mapConfig.minX..mapConfig.maxX step mapConfig.tileSize) {
                for (y in mapConfig.minY..mapConfig.maxY step mapConfig.tileSize) {
                    for (floor in mapConfig.lowestFloor..mapConfig.highestFloor) {
                        try {
                            val normalTile = apiService.getTile(
                                request = GetTileRequest(
                                    x = x,
                                    y = y,
                                    floor = floor,
                                    terrainType = TerrainType.NORMAL
                                )
                            ).bytes()

                            val waypointCostTile = apiService.getTile(
                                request = GetTileRequest(
                                    x = x,
                                    y = y,
                                    floor = floor,
                                    terrainType = TerrainType.WAYPOINT_COST
                                )
                            ).bytes()

                            val normalTileResult = fileStorage.saveFile(
                                fileName = "${TerrainType.NORMAL.name}_${x}_${y}_${floor}.png",
                                fileData = normalTile
                            )

                            if (normalTileResult.isFailure) {
                                return Result.failure(
                                    exception = normalTileResult.exceptionOrNull()
                                        ?: RuntimeException("Failed to save normal tile")
                                )
                            }

                            val waypointCostTileResult = fileStorage.saveFile(
                                fileName = "${TerrainType.WAYPOINT_COST.name}_${x}_${y}_${floor}.png",
                                fileData = waypointCostTile
                            )

                            if (waypointCostTileResult.isFailure) {
                                return Result.failure(
                                    exception = waypointCostTileResult.exceptionOrNull()
                                        ?: RuntimeException("Failed to save waypoint cost tile")
                                )
                            }
                        } catch (e: HttpException) {
                            if (e.code() == 404) {
                                continue
                            } else {
                                throw e
                            }
                        }
                    }
                }
            }

            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}