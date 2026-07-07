package app.aventurine.jetmap.data.network.api

import app.aventurine.jetmap.data.models.ladder.GetLaddersResponse
import retrofit2.http.POST

interface LadderApiService {

    @POST("ladders")
    suspend fun getLadders(): GetLaddersResponse
}