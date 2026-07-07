package app.aventurine.jetmapdemo.data.repositories

import app.aventurine.jetmapdemo.data.models.Entity
import app.aventurine.jetmapdemo.data.models.LocalEntity
import app.aventurine.jetmapdemo.data.models.RemoteEntity
import app.aventurine.jetmapdemo.data.room.BaseDao

abstract class EntityRepository<TEntity, TLocalEntity, TRemoteEntity>(
    protected val dao: BaseDao<TLocalEntity>
) : BaseRepository<TEntity>() where TEntity : Entity,
                                    TLocalEntity : LocalEntity,
                                    TRemoteEntity : RemoteEntity {

    @Suppress("UNCHECKED_CAST")
    suspend fun persist(entities: Collection<TLocalEntity>) {
        dao.insert(localEntities = entities)
    }
}