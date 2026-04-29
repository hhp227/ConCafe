package com.hhp227.concafe.domain.model

data class Cafe(
    val id: String,
    val name: String,
    val desc: String,
    val region: Region,
    val thumbnailImage: String?,
    val ratingAvg: Double,
    val reviewCount: Int,
    val approved: Boolean,
    val conceptType: String,
    val ownerIds: List<String> = emptyList(),
    val socialMedia: Map<String, String> = emptyMap(),
    val reservationUrl: String? = null,
    val tableCounts: TableCounts = TableCounts()
)

data class TableCounts(
    val current: Int = 0,
    val total: Int = 0
)
