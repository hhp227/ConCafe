package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import com.hhp227.concafe.domain.model.CafeDetail

data class CafeInfoEditUiState(
    val galleryMaxCount: Int = 3,
    val detail: CafeDetail? = null,
    val isRegistrationMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val cafeName: String = "",
    val cafeDescription: String = "",
    val representativeImageUrl: String? = null,
    val galleryImages: List<String> = emptyList(),
    val address: String = "",
    val mapLatitude: Double = 37.5665,
    val mapLongitude: Double = 126.9780,
    val contactNumber: String = "",
    val weekdayOpen: String = "",
    val weekdayClose: String = "",
    val weekendOpen: String = "",
    val weekendClose: String = "",
    val isImageRequiredAlertVisible: Boolean = false,
    val infoMessage: String? = null
) {
    val galleryLimitCount: Int
        get() = galleryImages.size
}
