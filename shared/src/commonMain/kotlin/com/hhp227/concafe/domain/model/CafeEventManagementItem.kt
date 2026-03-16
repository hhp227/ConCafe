package com.hhp227.concafe.domain.model

data class CafeEventManagementItem(
    val id: String,
    val cafeId: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val startDate: String,
    val endDate: String,
    val statusLabel: String,
    val isDimmed: Boolean
) {
    val periodText: String
        get() = "$startDate - $endDate"
}
