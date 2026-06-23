package app.aventurine.jetmapdemo.di

import app.aventurine.jetmapdemo.data.models.marker.MarkerDao
import app.aventurine.jetmapdemo.data.models.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.models.marker.MarkerRepositoryImpl
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
}