package app.aventurine.jetmapdemo.di

import app.aventurine.jetmap.data.dataStore.DataStoreManagerImpl
import app.aventurine.jetmap.data.fileStorage.FileStorageImpl
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Singleton
    @Binds
    abstract fun bindDataStoreManager(dataStoreManagerImpl: DataStoreManagerImpl): DataStoreManager

    @Binds
    abstract fun bindFileStorage(fileStorageImpl: FileStorageImpl): FileStorage
}