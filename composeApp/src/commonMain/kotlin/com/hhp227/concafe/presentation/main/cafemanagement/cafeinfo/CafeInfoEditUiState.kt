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
    val representativeImageTitle: String = "대표 이미지",
    val representativeImageUrl: String? = null,
    val galleryImages: List<String> = emptyList(),
    val address: String = "",
    val contactNumber: String = "",
    val weekdayOpen: String = "",
    val weekdayClose: String = "",
    val weekendOpen: String = "",
    val weekendClose: String = "",
    val isImageRequiredAlertVisible: Boolean = false,
    val infoMessage: String? = null
) {
    val galleryLimitText: String
        get() = "${galleryImages.size} / $galleryMaxCount"

    val screenTitle: String
        get() = if (isRegistrationMode) "새 카페 등록" else "카페 정보 관리"

    val submitButtonText: String
        get() = if (isRegistrationMode) "등록 신청하기" else "변경사항 저장"
}
