package app.aventurine.jetmap.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import app.aventurine.jetmap.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmap.data.room.BaseDao
import app.aventurine.jetmap.ui.JetMapConfig

@Dao
interface MarkerDao : BaseDao<MarkerLocalEntity> {
    @Query("SELECT * FROM marker")
    suspend fun getAll(): List<MarkerLocalEntity>

    @Query("SELECT * FROM marker WHERE x BETWEEN :x - 10 AND :x + 10 AND y BETWEEN :y - 10 AND :y + 10 AND floor = :z LIMIT 1")
    suspend fun get(x: Int, y: Int, z: Int): MarkerLocalEntity?

    @Query("SELECT * FROM marker WHERE x BETWEEN :startX AND :endX AND y BETWEEN :startY AND :endY AND floor = :floorId")
    suspend fun getMarkersByCoordinates(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        floorId: Int
    ): List<MarkerLocalEntity>

    suspend fun getMarkersByCoordinates(
        coordinates: JetMapConfig.Coordinates,
        floorId: Int
    ): List<MarkerLocalEntity> = getMarkersByCoordinates(
        startX = coordinates.startX,
        startY = coordinates.startY,
        endX = coordinates.endX,
        endY = coordinates.endY,
        floorId = floorId
    )

    @Query("SELECT * FROM marker WHERE x LIKE :query OR y LIKE :query OR description LIKE :query LIMIT 20")
    suspend fun search(query: String): List<MarkerLocalEntity>
}