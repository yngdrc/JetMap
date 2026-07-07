package app.aventurine.jetmapdemo.data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigEntity
import app.aventurine.jetmapdemo.data.models.config.serializer.MapConfigJsonSerializer
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class DataStoreManager(private val context: Context) {
    private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "config"
    )

    private val Context.mapConfigDataStore: DataStore<MapConfigLocalEntity> by dataStore(
        fileName = "mapConfig.json",
        serializer = MapConfigJsonSerializer
    )

    val preferencesFlow: Flow<Preferences> = context.preferencesDataStore.data
    val mapConfigFlow: Flow<MapConfigLocalEntity> = context.mapConfigDataStore.data

    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T {
        return getOrNull(key = key) ?: defaultValue
    }

    suspend fun <T> getOrNull(key: Preferences.Key<T>): T? {
        return preferencesFlow.firstOrNull()?.get(key = key)
    }

    suspend fun get(defaultValue: MapConfigEntity): MapConfigEntity {
        return getOrNull() ?: defaultValue
    }

    suspend fun getOrNull(): MapConfigEntity? {
        val mapConfigLocalEntity = mapConfigFlow.firstOrNull() ?: return null
        if (mapConfigLocalEntity !is MapConfigLocalEntity.Default) {
            return null
        }

        return MapConfigEntity(
            lowestFloor = mapConfigLocalEntity.lowestFloor,
            baseFloor = mapConfigLocalEntity.baseFloor,
            highestFloor = mapConfigLocalEntity.highestFloor,
            tileSize = mapConfigLocalEntity.tileSize,
            minX = mapConfigLocalEntity.minX,
            minY = mapConfigLocalEntity.minY,
            maxX = mapConfigLocalEntity.maxX,
            maxY = mapConfigLocalEntity.maxY,
            width = mapConfigLocalEntity.width,
            height = mapConfigLocalEntity.height
        )
    }

    suspend fun <T> update(key: Preferences.Key<T>, value: T?) {
        context.preferencesDataStore.edit { mutablePreferences ->
            if (value == null) {
                mutablePreferences.remove(key = key)
                return@edit
            }

            mutablePreferences[key] = value
        }
    }

    suspend fun update(mapConfig: MapConfigLocalEntity) {
        context.mapConfigDataStore.updateData { _ ->
            mapConfig
        }
    }
}