package app.aventurine.jetmapdemo.data.models.marker

import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerRemoteEntity

data class GetMarkersResponse(
    val markers: List<MarkerRemoteEntity>
)