package app.aventurine.jetmapdemo.data.base

abstract class RemoteEntity {
    abstract fun toLocalEntity(): LocalEntity
    abstract fun toEntity(): Entity
}