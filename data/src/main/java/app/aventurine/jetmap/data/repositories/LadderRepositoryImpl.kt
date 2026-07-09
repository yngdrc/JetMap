package app.aventurine.jetmap.data.repositories

import app.aventurine.jetmap.data.mappers.toEntity
import app.aventurine.jetmap.data.mappers.toLocalEntity
import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.data.room.dao.LadderDao
import app.aventurine.jetmap.domain.models.LadderEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.ui.JetMapConfig
import javax.inject.Inject

class LadderRepositoryImpl @Inject constructor(
    private val ladderDao: LadderDao,
    private val apiService: JetMapApiService
) : LadderRepository() {
    override suspend fun getAllOnline(
        returnOnPersistError: Boolean
    ): Result<Collection<LadderEntity>> = try {
        val response = apiService.getLadders()
        val ladders = response.ladders.map { ladder ->
            ladder.toEntity()
        }

        val persistError = persist(entities = ladders).exceptionOrNull()
        if (returnOnPersistError && persistError != null) {
            return Result.failure(exception = persistError)
        }

        Result.success(value = ladders)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }

    override suspend fun getAllOffline(): Result<Collection<LadderEntity>> = try {
        val ladders = ladderDao.getAll().map { ladder ->
            ladder.toEntity()
        }

        Result.success(value = ladders)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }

    override suspend fun get(x: Int, y: Int, z: Int): LadderEntity? {
        return ladderDao.get(x = x, y = y, floor = z)?.toEntity()
    }

    override suspend fun getLaddersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): Collection<LadderEntity> {
        return ladderDao.getLaddersByCoordinates(
            coordinates = coordinates,
            floorId = floorId
        ).map { ladderLocalEntity -> ladderLocalEntity.toEntity() }
    }

    override suspend fun getConnectedLadder(x: Int, y: Int, floor: Int): LadderEntity? {
        return ladderDao.getConnectedLadder(
            x = x,
            y = y,
            floor = floor
        )?.toEntity()
    }

    override suspend fun persist(
        entities: List<LadderEntity>
    ): Result<Unit> = try {
        ladderDao.insert(
            localEntities = entities.map { entity ->
                entity.toLocalEntity()
            }
        )

        Result.success(value = Unit)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }
}