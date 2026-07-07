package app.aventurine.jetmapdemo.di

import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.dataStore.DataStoreManagerImpl
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.fileStorage.FileStorageImpl
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