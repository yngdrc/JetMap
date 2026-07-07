package app.aventurine.jetmapdemo.data.models

abstract class RemoteEntity {
    abstract fun toLocalEntity(): LocalEntity
    abstract fun toEntity(): Entity
}