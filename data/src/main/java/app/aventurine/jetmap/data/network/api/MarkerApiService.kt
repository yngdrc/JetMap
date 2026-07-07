package app.aventurine.jetmap.data.network.api

import app.aventurine.jetmap.data.models.marker.GetMarkersResponse
import retrofit2.http.POST

interface MarkerApiService {
    @POST("markers")
    suspend fun getMarkers(): GetMarkersResponse
}