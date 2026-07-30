package app.aventurine.jetmap.domain.dataStore

import androidx.datastore.preferences.core.Preferences
import app.aventurine.jetmap.domain.models.MapConfigEntity
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    val preferencesFlow: Flow<Preferences>
    val mapConfigFlow: Flow<MapConfigEntity?>

    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T
    suspend fun <T> getOrNull(key: Preferences.Key<T>): T?
    suspend fun get(defaultValue: MapConfigEntity): MapConfigEntity
    suspend fun getOrNull(): MapConfigEntity?
    suspend fun <T> update(key: Preferences.Key<T>, value: T?)
    suspend fun update(mapConfig: MapConfigEntity)
}