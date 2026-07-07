package app.aventurine.jetmapdemo.data.repositories

import app.aventurine.jetmapdemo.data.models.Entity

abstract class BaseRepository<TEntity : Entity> {
    abstract suspend fun getAll(): Collection<TEntity>
}