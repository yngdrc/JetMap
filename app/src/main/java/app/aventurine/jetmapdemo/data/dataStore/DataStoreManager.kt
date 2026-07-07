package app.aventurine.jetmapdemo.data.dataStore

import androidx.datastore.preferences.core.Preferences
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigEntity
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    val preferencesFlow: Flow<Preferences>
    val mapConfigFlow: Flow<MapConfigLocalEntity>

    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T
    suspend fun <T> getOrNull(key: Preferences.Key<T>): T?
    suspend fun get(defaultValue: MapConfigEntity): MapConfigEntity
    suspend fun getOrNull(): MapConfigEntity?
    suspend fun <T> update(key: Preferences.Key<T>, value: T?)
    suspend fun update(mapConfig: MapConfigLocalEntity)
}