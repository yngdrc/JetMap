package app.aventurine.jetmap.data.models.marker

import app.aventurine.jetmap.data.models.marker.entities.MarkerRemoteEntity

data class GetMarkersResponse(
    val markers: List<MarkerRemoteEntity>
)