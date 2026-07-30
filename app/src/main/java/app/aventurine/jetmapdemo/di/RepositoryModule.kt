package app.aventurine.jetmapdemo.di

import app.aventurine.jetmap.data.repositories.LadderRepositoryImpl
import app.aventurine.jetmap.data.repositories.MarkerRepositoryImpl
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.repositories.MarkerRepository
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