package app.aventurine.jetmapdemo.di

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import app.aventurine.jetmapdemo.MarkerExtractor
import app.aventurine.jetmapdemo.data.models.marker.MarkerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager {
        return context.assets
    }

    @Provides
    fun provideResources(@ApplicationContext context: Context): Resources {
        return context.resources
    }
}