package app.aventurine.jetmap.data.room.dao

import androidx.room.Dao
import app.aventurine.jetmap.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmap.data.room.BaseDao

@Dao
interface LadderDao : BaseDao<LadderLocalEntity>