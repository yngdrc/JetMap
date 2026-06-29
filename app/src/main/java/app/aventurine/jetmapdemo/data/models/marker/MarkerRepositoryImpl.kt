package app.aventurine.jetmapdemo.data.models.marker

import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.toEntity
import javax.inject.Inject

class MarkerRepositoryImpl @Inject constructor(
    private val markerDao: MarkerDao,
) : MarkerRepository(dao = markerDao) {
    override suspend fun get(uid: String): MarkerEntity? {
        return markerDao.get(uid = uid)?.toEntity()
    }

    override suspend fun get(x: Int, y: Int, z: Int): MarkerEntity? {
        return markerDao.get(x = x, y = y, z = z)?.toEntity()
    }

    override suspend fun getAll(): Collection<MarkerEntity> {
        return markerDao.getAll().map { markerLocalEntity -> markerLocalEntity.toEntity() }
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
}