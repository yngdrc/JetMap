package app.aventurine.jetmapdemo.di

import app.aventurine.jetmap.data.services.sync.SyncServiceImpl
import app.aventurine.jetmap.domain.services.SyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Singleton
    @Binds
    abstract fun bindSyncService(syncServiceImpl: SyncServiceImpl): SyncService
}