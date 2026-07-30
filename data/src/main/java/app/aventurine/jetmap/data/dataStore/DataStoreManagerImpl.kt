package app.aventurine.jetmap.data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import app.aventurine.jetmap.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmap.data.models.config.serializer.MapConfigJsonSerializer
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.models.MapConfigEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DataStoreManager {
    private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "config"
    )

    private val Context.mapConfigDataStore: DataStore<MapConfigLocalEntity> by dataStore(
        fileName = "mapConfig.json",
        serializer = MapConfigJsonSerializer
    )

    override val preferencesFlow: Flow<Preferences> = context.preferencesDataStore.data
    override val mapConfigFlow: Flow<MapConfigEntity?> = context.mapConfigDataStore.data.map { mapConfigLocalEntity ->
        if (mapConfigLocalEntity !is MapConfigLocalEntity.Default) {
            return@map null
        }

        MapConfigEntity(
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

    override suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T {
        return getOrNull(key = key) ?: defaultValue
    }

    override suspend fun <T> getOrNull(key: Preferences.Key<T>): T? {
        return preferencesFlow.firstOrNull()?.get(key = key)
    }

    override suspend fun get(defaultValue: MapConfigEntity): MapConfigEntity {
        return getOrNull() ?: defaultValue
    }

    override suspend fun getOrNull(): MapConfigEntity? {
        return mapConfigFlow.firstOrNull()
    }

    override suspend fun <T> update(key: Preferences.Key<T>, value: T?) {
        context.preferencesDataStore.edit { mutablePreferences ->
            if (value == null) {
                mutablePreferences.remove(key = key)
                return@edit
            }

            mutablePreferences[key] = value
        }
    }

    override suspend fun update(mapConfig: MapConfigEntity) {
        context.mapConfigDataStore.updateData { _ ->
            MapConfigLocalEntity.Default(
                lowestFloor = mapConfig.lowestFloor,
                baseFloor = mapConfig.baseFloor,
                highestFloor = mapConfig.highestFloor,
                tileSize = mapConfig.tileSize,
                minX = mapConfig.minX,
                minY = mapConfig.minY,
                maxX = mapConfig.maxX,
                maxY = mapConfig.maxY,
                width = mapConfig.width,
                height = mapConfig.height
            )
        }
    }
}