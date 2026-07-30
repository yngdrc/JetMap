package app.aventurine.jetmap.domain.repositories

import app.aventurine.jetmap.domain.models.LadderEntity
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.ui.JetMapConfig

abstract class LadderRepository : BaseRepository<LadderEntity>() {
    abstract suspend fun get(x: Int, y: Int, z: Int): LadderEntity?

    abstract suspend fun getLaddersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): Collection<LadderEntity>

    abstract suspend fun getConnectedLadder(
        x: Int,
        y: Int,
        floor: Int
    ): LadderEntity?
}