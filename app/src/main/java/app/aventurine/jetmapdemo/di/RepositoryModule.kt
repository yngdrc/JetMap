package app.aventurine.jetmapdemo.di

import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.room.dao.LadderDao
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepositoryImpl
import app.aventurine.jetmapdemo.data.room.dao.MarkerDao
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideMarkerRepository(
        markerDao: MarkerDao,
    ): MarkerRepository {
        return MarkerRepositoryImpl(
            markerDao = markerDao,
        )
    }

    @Provides
    fun provideLadderRepository(
        ladderDao: LadderDao,
        apiService: JetMapApiService
    ): LadderRepository {
        return LadderRepositoryImpl(
            dao = ladderDao,
            apiService = apiService
        )
    }
}