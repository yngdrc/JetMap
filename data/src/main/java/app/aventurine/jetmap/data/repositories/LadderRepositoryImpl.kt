package app.aventurine.jetmap.data.repositories

import app.aventurine.jetmap.data.models.ladder.entities.LadderLocalEntity
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
    override suspend fun getAll(): Collection<LadderEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun getLaddersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): List<LadderEntity> {
        return listOf()
//        val response = apiService.getLadders(
//            request = GetLaddersRequest(
//                fromX = coordinates.startX,
//                fromY = coordinates.startY,
//                toX = coordinates.endX,
//                toY = coordinates.endY,
//                floor = floorId
//            )
//        )
//
//        return response.ladders.map { ladderRemoteEntity ->
//            ladderRemoteEntity.toEntity()
//        }
    }

    override suspend fun persist(entities: List<LadderEntity>) {
        ladderDao.insert(
            localEntities = entities.map { entity ->
                LadderLocalEntity(
                    x = entity.x,
                    y = entity.y,
                    floor = entity.floor
                )
            }
        )
    }
}