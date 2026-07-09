package app.aventurine.jetmap.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import app.aventurine.jetmap.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmap.data.room.BaseDao
import app.aventurine.jetmap.ui.JetMapConfig

@Dao
interface LadderDao : BaseDao<LadderLocalEntity> {
    @Query("SELECT * FROM ladder")
    suspend fun getAll(): List<LadderLocalEntity>

    @Query("SELECT * FROM ladder WHERE (x BETWEEN :x - 10 AND :x + 10) AND (y BETWEEN :y - 10 AND :y + 10) AND floor = :floor LIMIT 1")
    suspend fun get(x: Int, y: Int, floor: Int): LadderLocalEntity?

    @Query("SELECT * FROM ladder WHERE (x BETWEEN :startX AND :endX) AND (y BETWEEN :startY AND :endY) AND floor = :floor")
    suspend fun getLaddersByCoordinates(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        floor: Int
    ): List<LadderLocalEntity>

    @Query("SELECT * FROM ladder WHERE x = :x AND y = :y AND (floor = :floor - 1 OR floor = :floor + 1)")
    suspend fun getConnectedLadder(
        x: Int,
        y: Int,
        floor: Int
    ): LadderLocalEntity?

    suspend fun getLaddersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): List<LadderLocalEntity> = getLaddersByCoordinates(
        startX = coordinates.startX,
        startY = coordinates.startY,
        endX = coordinates.endX,
        endY = coordinates.endY,
        floor = floorId
    )
}