package app.aventurine.jetmap.domain.repositories

import app.aventurine.jetmap.domain.models.Entity

abstract class BaseRepository<TEntity : Entity> {
    abstract suspend fun getAll(): Collection<TEntity>
    abstract suspend fun persist(entities: List<TEntity>)
}