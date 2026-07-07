package app.aventurine.jetmapdemo.data.network

import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigRemoteEntity
import app.aventurine.jetmapdemo.data.network.api.LadderApiService
import app.aventurine.jetmapdemo.data.network.api.MarkerApiService
import app.aventurine.jetmapdemo.data.network.api.TileApiService
import retrofit2.http.GET

@Api("https://59c0-185-234-91-205.ngrok-free.app")
interface JetMapApiService : TileApiService, MarkerApiService, LadderApiService {
    @GET("version")
    suspend fun getVersion(): String

    @GET("mapConfig")
    suspend fun getMapConfig(): MapConfigRemoteEntity
}