package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import com.hhp227.concafe.domain.model.CafeDetail

data class CafeInfoEditUiState(
    val detail: CafeDetail? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val cafeName: String = "",
    val cafeDescription: String = "",
    val representativeImageTitle: String = "대표 이미지",
    val representativeImageUrl: String? = null,
    val galleryImages: List<String> = emptyList(),
    val address: String = "",
    val contactNumber: String = "",
    val weekdayOpen: String = "",
    val weekdayClose: String = "",
    val weekendOpen: String = "",
    val weekendClose: String = "",
    val infoMessage: String? = null
) {
    val galleryLimitText: String
        get() = "${galleryImages.size} / 10"
}
