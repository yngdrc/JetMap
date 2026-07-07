package app.aventurine.jetmap.data.models.marker.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.aventurine.jetmap.data.models.LocalEntity
import app.aventurine.jetmap.domain.models.MarkerEntity

@Entity(tableName = "marker", primaryKeys = ["x", "y", "floor"])
data class MarkerLocalEntity(
    @ColumnInfo(name = "x") val x: Int,
    @ColumnInfo(name = "y") val y: Int,
    @ColumnInfo(name = "floor") val floor: Int,
    @ColumnInfo(name = "iconId") val iconId: Int,
    @ColumnInfo(name = "description") val description: String,
) : LocalEntity()

fun MarkerLocalEntity.toEntity(): MarkerEntity {
    return MarkerEntity(
        x = this.x,
        y = this.y,
        floor = this.floor,
        iconId = this.iconId,
        description = this.description
    )
}