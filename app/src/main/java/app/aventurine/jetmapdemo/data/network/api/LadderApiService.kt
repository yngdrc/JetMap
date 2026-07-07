package app.aventurine.jetmapdemo.data.network.api

import app.aventurine.jetmapdemo.data.models.ladder.GetLaddersRequest
import app.aventurine.jetmapdemo.data.models.ladder.GetLaddersResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LadderApiService {

    @POST("ladders")
    suspend fun getLadders(): GetLaddersResponse
}