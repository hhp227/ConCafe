package com.hhp227.concafe.domain.model

data class CastUpsert(
    val castId: String? = null,
    val cafeId: String? = null,
    val name: String,
    val conceptRole: String,
    val birthday: String? = null,
    val introduction: String,
    val workingDays: List<String> = emptyList()
)
