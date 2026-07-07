package app.aventurine.jetmapdemo.di

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.fileStorage.FileStorageImpl
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.services.sync.SyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager {
        return context.assets
    }

    @Provides
    fun provideResources(@ApplicationContext context: Context): Resources {
        return context.resources
    }

    @Singleton
    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context = context)
    }

    @Singleton
    @Provides
    fun provideSyncService(
        dataStoreManager: DataStoreManager,
        jetMapApiService: JetMapApiService,
        fileStorage: FileStorage,
        markerRepository: MarkerRepository,
        ladderRepository: LadderRepository
    ): SyncService {
        return SyncService(
            dataStoreManager = dataStoreManager,
            apiService = jetMapApiService,
            fileStorage = fileStorage,
            markerRepository = markerRepository,
            ladderRepository = ladderRepository
        )
    }

    @Provides
    fun provideFileStorage(
        @ApplicationContext context: Context
    ): FileStorage {
        return FileStorageImpl(context = context)
    }
}