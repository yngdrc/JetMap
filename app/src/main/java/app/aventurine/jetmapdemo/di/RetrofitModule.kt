package app.aventurine.jetmapdemo.di

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.data.network.host
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    fun provideJetMapApiService(): JetMapApiService {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(JetMapApiService::class.host)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JetMapApiService::class.java)
    }
}