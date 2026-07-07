package app.aventurine.jetmapdemo.data.repositories.ladder

import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.room.dao.LadderDao
import app.aventurine.jetmapdemo.data.repositories.EntityRepository
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderEntity
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderRemoteEntity

abstract class LadderRepository(
    dao: LadderDao
) : EntityRepository<LadderEntity, LadderLocalEntity, LadderRemoteEntity>(dao = dao) {
    abstract suspend fun getLaddersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): List<LadderEntity>
}