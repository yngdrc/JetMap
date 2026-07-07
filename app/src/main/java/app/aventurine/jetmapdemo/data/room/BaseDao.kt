package app.aventurine.jetmapdemo.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import app.aventurine.jetmapdemo.data.models.LocalEntity

@Dao
interface BaseDao<TEntity : LocalEntity> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localEntities: Collection<TEntity>)

    @Update
    suspend fun update(localEntities: Collection<TEntity>)

    @Delete
    suspend fun delete(localEntities: Collection<TEntity>)
}