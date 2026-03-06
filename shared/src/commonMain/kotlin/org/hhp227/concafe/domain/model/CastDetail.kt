package org.hhp227.concafe.domain.model

data class CastDetail(
    val cast: Cast,
    val cafe: Cafe,
    val images: List<String>,
    val schedule: List<CastSchedule>
)
