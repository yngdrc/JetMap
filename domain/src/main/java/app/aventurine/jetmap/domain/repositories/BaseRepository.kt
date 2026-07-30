package app.aventurine.jetmap.domain.repositories

import app.aventurine.jetmap.domain.models.Entity

abstract class BaseRepository<TEntity : Entity> {
    abstract suspend fun getAllOnline(
        returnOnPersistError: Boolean
    ): Result<Collection<TEntity>>

    abstract suspend fun getAllOffline(): Result<Collection<TEntity>>
    abstract suspend fun persist(entities: List<TEntity>): Result<Unit>
}