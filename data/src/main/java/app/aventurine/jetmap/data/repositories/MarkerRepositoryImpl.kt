package app.aventurine.jetmap.data.repositories

import app.aventurine.jetmap.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmap.data.models.marker.entities.toEntity
import app.aventurine.jetmap.data.room.dao.MarkerDao
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.ui.JetMapConfig
import javax.inject.Inject

class MarkerRepositoryImpl @Inject constructor(
    private val markerDao: MarkerDao,
) : MarkerRepository() {
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

    override suspend fun persist(entities: List<MarkerEntity>) {
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
    }
}