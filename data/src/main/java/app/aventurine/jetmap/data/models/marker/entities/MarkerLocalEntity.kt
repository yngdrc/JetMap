package app.aventurine.jetmap.data.models.marker.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.aventurine.jetmap.data.models.LocalEntity

@Entity(tableName = "marker", primaryKeys = ["x", "y", "floor"])
data class MarkerLocalEntity(
    @ColumnInfo(name = "x") val x: Int,
    @ColumnInfo(name = "y") val y: Int,
    @ColumnInfo(name = "floor") val floor: Int,
    @ColumnInfo(name = "iconId") val iconId: Int,
    @ColumnInfo(name = "description") val description: String,
) : LocalEntity()