package app.aventurine.jetmap.data.network.api

import app.aventurine.jetmap.data.models.config.entities.MapConfigRemoteEntity
import app.aventurine.jetmap.data.network.Api
import retrofit2.http.GET

@Api("https://a0d5-185-234-91-125.ngrok-free.app")
interface JetMapApiService : TileApiService, MarkerApiService, LadderApiService {
    @GET("version")
    suspend fun getVersion(): String

    @GET("mapConfig")
    suspend fun getMapConfig(): MapConfigRemoteEntity
}