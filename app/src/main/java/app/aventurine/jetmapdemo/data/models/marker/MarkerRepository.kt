package app.aventurine.jetmapdemo.data.models.marker

import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.base.EntityRepository
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerRemoteEntity

abstract class MarkerRepository(
    dao: MarkerDao
) : EntityRepository<MarkerEntity, MarkerLocalEntity, MarkerRemoteEntity>(dao = dao) {
    abstract suspend fun getMarkersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): Collection<MarkerEntity>
}