package org.hhp227.concafe.domain.model

data class CafeDetail(
    val cafe: Cafe,
    val images: List<String>,
    val casts: List<Cast>,
    val menus: List<Menu>,
    val goods: List<Goods>,
    val notices: List<Notice>,
    val businessHours: String,
    val phoneNumber: String
)
