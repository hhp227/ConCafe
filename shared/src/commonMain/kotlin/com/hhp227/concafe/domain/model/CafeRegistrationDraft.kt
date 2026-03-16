package com.hhp227.concafe.domain.model

data class CafeRegistrationDraft(
    val name: String,
    val description: String,
    val region: Region,
    val thumbnailImage: String?,
    val conceptType: String,
    val businessHours: String,
    val phoneNumber: String
)
