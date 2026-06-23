package app.aventurine.jetmapdemo.data.models.marker

import androidx.room.Dao
import androidx.room.Query
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.base.BaseDao
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerLocalEntity

@Dao
interface MarkerDao : BaseDao<MarkerLocalEntity> {

    @Query("SELECT * FROM marker WHERE uid = :uid")
    suspend fun get(uid: String): MarkerLocalEntity

    @Query("SELECT * FROM marker")
    suspend fun getAll(): List<MarkerLocalEntity>

    @Query("SELECT * FROM marker WHERE x BETWEEN :startX AND :endX AND y BETWEEN :startY AND :endY AND floorId = :floorId")
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
}