package app.aventurine.jetmap.data.network.api

import app.aventurine.jetmap.data.models.tile.GetTileRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface TileApiService {

    @POST("tile")
    suspend fun getTile(
        @Body request: GetTileRequest
    ): ResponseBody
}