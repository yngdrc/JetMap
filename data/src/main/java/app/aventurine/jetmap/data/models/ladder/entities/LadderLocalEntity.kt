package app.aventurine.jetmap.data.models.ladder.entities

import androidx.room.Entity
import app.aventurine.jetmap.data.models.LocalEntity

@Entity(tableName = "ladder", primaryKeys = ["x", "y", "floor"])
data class LadderLocalEntity(
    val x: Int,
    val y: Int,
    val floor: Int,
) : LocalEntity()