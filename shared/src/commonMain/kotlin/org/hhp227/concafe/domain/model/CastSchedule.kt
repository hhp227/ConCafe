package com.hhp227.concafe.domain.model

data class CastSchedule(
    val id: String,
    val castId: String,
    val cafeId: String,
    val date: String,
    val startTime: String,
    val endTime: String
)
