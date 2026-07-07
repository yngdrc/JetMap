package app.aventurine.jetmapdemo.data.network.api

import app.aventurine.jetmapdemo.data.models.marker.GetMarkersResponse
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerRemoteEntity
import retrofit2.http.GET
import retrofit2.http.POST

interface MarkerApiService {
    @POST("markers")
    suspend fun getMarkers(): GetMarkersResponse
}