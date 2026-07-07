package app.aventurine.jetmapdemo.di

import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepositoryImpl
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMarkerRepository(markerRepositoryImpl: MarkerRepositoryImpl): MarkerRepository

    @Binds
    abstract fun bindLadderRepository(ladderRepositoryImpl: LadderRepositoryImpl): LadderRepository
}