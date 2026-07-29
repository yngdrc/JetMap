package app.aventurine.jetmap.data.repositories

import app.aventurine.jetmap.data.mappers.toEntity
import app.aventurine.jetmap.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.data.room.dao.MarkerDao
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.ui.JetMapConfig
import javax.inject.Inject

class MarkerRepositoryImpl @Inject constructor(
    private val markerDao: MarkerDao,
    private val apiService: JetMapApiService
) : MarkerRepository() {
    override suspend fun getAllOnline(
        returnOnPersistError: Boolean
    ): Result<Collection<MarkerEntity>> = try {
        val response = apiService.getMarkers()
        val markers = response.markers.map { marker ->
            marker.toEntity()
        }

        val persistError = persist(entities = markers).exceptionOrNull()
        if (returnOnPersistError && persistError != null) {
            return Result.failure(exception = persistError)
        }

        Result.success(value = markers)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }

    override suspend fun getAllOffline(): Result<Collection<MarkerEntity>> = try {
        val markers = markerDao.getAll().map { markerLocalEntity ->
            markerLocalEntity.toEntity()
        }

        Result.success(value = markers)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }

    override suspend fun get(
        x: Int,
        y: Int,
        z: Int,
        tapArea: Float
    ): MarkerEntity? {
        return markerDao.get(x = x, y = y, z = z, tapArea = tapArea)?.toEntity()
    }

    override suspend fun getMarkersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): Collection<MarkerEntity> {
        return markerDao.getMarkersByCoordinates(
            coordinates = coordinates,
            floorId = floorId
        ).map { markerLocalEntity -> markerLocalEntity.toEntity() }
    }

    override suspend fun search(query: String): List<MarkerEntity> {
        return markerDao.search(
            query = "%$query%"
        ).map { markerLocalEntity -> markerLocalEntity.toEntity() }
    }

    override suspend fun persist(entities: List<MarkerEntity>): Result<Unit> = try {
        markerDao.insert(
            localEntities = entities.map { entity ->
                MarkerLocalEntity(
                    x = entity.x,
                    y = entity.y,
                    floor = entity.floor,
                    iconId = entity.iconId,
                    description = entity.description,
                )
            }
        )

        Result.success(value = Unit)
    } catch (e: Exception) {
        Result.failure(exception = e)
    }
}