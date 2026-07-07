package app.aventurine.jetmapdemo.data.models.marker.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.aventurine.jetmapdemo.data.models.LocalEntity

@Entity(tableName = "marker", primaryKeys = ["x", "y", "floorId"])
data class MarkerLocalEntity(
    @ColumnInfo(name = "x") val x: Int,
    @ColumnInfo(name = "y") val y: Int,
    @ColumnInfo(name = "floorId") val floorId: Int,
    @ColumnInfo(name = "iconId") val iconId: Int,
    @ColumnInfo(name = "description") val description: String,
) : LocalEntity()

fun MarkerLocalEntity.toEntity(): MarkerEntity {
    return MarkerEntity(
        x = this.x,
        y = this.y,
        floorId = this.floorId,
        iconId = this.iconId,
        description = this.description
    )
}