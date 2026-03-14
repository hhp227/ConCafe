package com.hhp227.concafe.domain.model

data class CastScheduleUpdate(
    val castId: String,
    val date: String,
    val status: CastScheduleStatus,
    val startTime: String? = null,
    val endTime: String? = null
)
