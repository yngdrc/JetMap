package app.aventurine.jetmapdemo.data.base

import androidx.room.Database
import androidx.room.RoomDatabase
import app.aventurine.jetmapdemo.data.models.marker.MarkerDao
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerLocalEntity

@Database(entities = [MarkerLocalEntity::class], version = 1, exportSchema = false)
abstract class JetMapDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    companion object {
        const val DATABASE_NAME: String = "jet_map_database"
    }
}