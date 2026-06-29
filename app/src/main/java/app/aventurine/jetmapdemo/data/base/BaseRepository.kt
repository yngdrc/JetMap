package app.aventurine.jetmapdemo.data.base

abstract class BaseRepository<TEntity : Entity> {
    abstract suspend fun get(uid: String): TEntity?
    abstract suspend fun getAll(): Collection<TEntity>
}