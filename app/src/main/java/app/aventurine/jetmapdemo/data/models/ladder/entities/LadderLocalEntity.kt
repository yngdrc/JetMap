package app.aventurine.jetmapdemo.data.models.ladder.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.aventurine.jetmapdemo.data.models.LocalEntity

@Entity(tableName = "ladder", primaryKeys = ["x", "y", "floor"])
data class LadderLocalEntity(
    val x: Int,
    val y: Int,
    val floor: Int,
) : LocalEntity()