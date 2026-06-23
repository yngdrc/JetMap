package app.aventurine.jetmapdemo.data.base

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