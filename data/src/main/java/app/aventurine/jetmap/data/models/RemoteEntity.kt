package app.aventurine.jetmap.data.models

import app.aventurine.jetmap.domain.models.Entity

abstract class RemoteEntity {
    abstract fun toLocalEntity(): LocalEntity
    abstract fun toEntity(): Entity
}