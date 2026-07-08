package app.aventurine.jetmap.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import app.aventurine.jetmap.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmap.data.room.BaseDao

@Dao
interface LadderDao : BaseDao<LadderLocalEntity> {
    @Query("SELECT * FROM ladder")
    suspend fun getAll(): List<LadderLocalEntity>
}