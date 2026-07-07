package app.aventurine.jetmapdemo.data.room.dao

import androidx.room.Dao
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmapdemo.data.room.BaseDao

@Dao
interface LadderDao : BaseDao<LadderLocalEntity>