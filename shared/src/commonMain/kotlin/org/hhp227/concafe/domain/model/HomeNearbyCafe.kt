package org.hhp227.concafe.domain.model

data class HomeNearbyCafe(
    val id: String,
    val name: String,
    val rating: Double,
    val location: String,
    val distance: String,
    val imageUrl: String?
)
