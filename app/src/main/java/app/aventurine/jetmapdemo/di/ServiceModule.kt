package app.aventurine.jetmapdemo.di

import app.aventurine.jetmapdemo.data.services.sync.SyncService
import app.aventurine.jetmapdemo.data.services.sync.SyncServiceImpl
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