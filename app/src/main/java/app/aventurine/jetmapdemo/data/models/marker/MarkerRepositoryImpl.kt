package app.aventurine.jetmapdemo.data.models.marker

import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.toEntity
import javax.inject.Inject

class MarkerRepositoryImpl @Inject constructor(
    private val markerDao: MarkerDao,
) : MarkerRepository(dao = markerDao) {
    override suspend fun get(uid: String): MarkerEntity {
        return markerDao.get(uid = uid).toEntity()
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
}