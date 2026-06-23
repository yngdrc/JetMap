package app.aventurine.jetmapdemo.di

import android.content.Context
import androidx.room.Room
import app.aventurine.jetmapdemo.data.base.JetMapDatabase
import app.aventurine.jetmapdemo.data.models.marker.MarkerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTibiaBuddyDatabase(
        @ApplicationContext context: Context
    ): JetMapDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = JetMapDatabase::class.java,
            name = JetMapDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMarkerDao(
        jetMapDatabase: JetMapDatabase
    ): MarkerDao {
        return jetMapDatabase.markerDao()
    }
}