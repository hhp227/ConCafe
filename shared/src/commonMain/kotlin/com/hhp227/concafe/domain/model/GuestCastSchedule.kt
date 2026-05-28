package com.hhp227.concafe.domain.model

data class GuestCastSchedule(
    val id: String,
    val cafeId: String,
    val date: String,
    val name: String,
    val profileImage: String? = null,
    val startTime: String,
    val endTime: String,
    val memo: String? = null
)

data class GuestCastScheduleUpsert(
    val id: String? = null,
    val cafeId: String,
    val date: String,
    val name: String,
    val profileImage: String? = null,
    val startTime: String,
    val endTime: String,
    val memo: String? = null
)
