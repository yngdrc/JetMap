package app.aventurine.jetmapdemo.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import app.aventurine.jetmapdemo.data.room.dao.LadderDao
import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmapdemo.data.room.dao.MarkerDao
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerLocalEntity

@Database(
    entities = [MarkerLocalEntity::class, LadderLocalEntity::class],
    version = 1,
    exportSchema = true
)
abstract class JetMapDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    abstract fun ladderDao(): LadderDao

    companion object {
        const val DATABASE_NAME: String = "jet_map_database"
    }
}