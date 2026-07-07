package app.aventurine.jetmapdemo.data.repositories.ladder

import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.room.dao.LadderDao
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderEntity
import javax.inject.Inject

class LadderRepositoryImpl @Inject constructor(
    dao: LadderDao,
    private val apiService: JetMapApiService
) : LadderRepository(dao = dao) {
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
}