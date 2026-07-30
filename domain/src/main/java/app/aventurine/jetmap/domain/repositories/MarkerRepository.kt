package app.aventurine.jetmap.domain.repositories

import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.ui.JetMapConfig

abstract class MarkerRepository : BaseRepository<MarkerEntity>() {
    abstract suspend fun get(
        x: Int,
        y: Int,
        z: Int,
        tapArea: Float
    ): MarkerEntity?

    abstract suspend fun getMarkersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): Collection<MarkerEntity>

    abstract suspend fun search(query: String): List<MarkerEntity>
}