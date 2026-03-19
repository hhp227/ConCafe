package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem

data class BannerEditUiState(
    val screenTitle: String = "새 배너 등록",
    val submitButtonText: String = "배너 등록하기",
    val imageSectionTitle: String = "배너 이미지 업로드",
    val imageGuideText: String = "권장 비율 16:9 (1080x600px)",
    val imageButtonText: String = "이미지 선택",
    val selectedImageLabel: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val selectedTarget: BannerTargetType = BannerTargetType.CAFE_DETAIL,
    val targetValue: String = "",
    val displayDays: Int = 5,
    val ownedCafeOptions: List<CafeManagementData.OwnedCafeSummary> = emptyList(),
    val selectedCafeId: String? = null,
    val selectedNoticeId: String? = null,
    val selectedEventId: String? = null,
    val selectorType: BannerSelectorType? = null,
    val selectorQuery: String = "",
    val noticeSelectorOptions: List<CafeNoticeManagementItem> = emptyList(),
    val eventSelectorOptions: List<CafeEventManagementItem> = emptyList(),
    val isSelectorLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val isImageRequiredAlertVisible: Boolean = false,
    val isSaving: Boolean = false,
    val infoMessage: String? = "현재 활성화된 배너 슬롯이 가득 찬 경우, 등록된 배너는 예약 상태(SCHEDULED)로 대기하며 기존 배너 종료 시 자동으로 노출됩니다."
) {
    val displayDaysLabel: String
        get() = "${displayDays}일"

    val targetFieldPlaceholder: String
        get() = selectedTarget.placeholder

    val selectorTitle: String
        get() = selectorType?.title.orEmpty()

    val selectorSearchPlaceholder: String
        get() = selectorType?.searchPlaceholder.orEmpty()

    val selectedCafeOption: CafeManagementData.OwnedCafeSummary?
        get() = selectedCafeId?.let { id -> ownedCafeOptions.firstOrNull { it.id == id } }

    val selectedContentTitle: String?
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> {
                selectedNoticeId?.let { id -> noticeSelectorOptions.firstOrNull { it.id == id }?.title }
            }
            BannerTargetType.EVENT_DETAIL -> {
                selectedEventId?.let { id -> eventSelectorOptions.firstOrNull { it.id == id }?.title }
            }
            else -> null
        }

    val selectedContentSubtitle: String?
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> {
                selectedNoticeId?.let { id -> noticeSelectorOptions.firstOrNull { it.id == id }?.displayDate }
            }
            BannerTargetType.EVENT_DETAIL -> {
                selectedEventId?.let { id -> eventSelectorOptions.firstOrNull { it.id == id }?.periodText }
            }
            else -> null
        }

    val filteredCafeSelectorOptions: List<CafeManagementData.OwnedCafeSummary>
        get() = if (selectorQuery.isBlank()) {
            ownedCafeOptions
        } else {
            ownedCafeOptions.filter {
                it.name.contains(selectorQuery, ignoreCase = true) ||
                    it.city.contains(selectorQuery, ignoreCase = true)
            }
        }

    val activeSelectorItemCount: Int
        get() = when (selectorType) {
            BannerSelectorType.CAFE -> filteredCafeSelectorOptions.size
            BannerSelectorType.NOTICE -> noticeSelectorOptions.size
            BannerSelectorType.EVENT -> eventSelectorOptions.size
            null -> 0
        }

    val isSaveEnabled: Boolean
        get() = title.isNotBlank() &&
            subtitle.isNotBlank() &&
            !selectedImageLabel.isNullOrBlank() &&
            targetValue.isNotBlank() &&
            !isSaving

    val targetSelectionLabel: String
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> "공지사항 선택"
            BannerTargetType.EVENT_DETAIL -> "이벤트 선택"
            else -> ""
        }

    val targetSelectionPlaceholder: String
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> "공지사항을 검색하고 선택해주세요"
            BannerTargetType.EVENT_DETAIL -> "이벤트를 검색하고 선택해주세요"
            else -> ""
        }
}

enum class BannerTargetType(
    val label: String,
    val placeholder: String
) {
    CAFE_DETAIL("카페 상세", "운영 카페를 선택해주세요"),
    EVENT_DETAIL("이벤트 상세", "이벤트를 검색하고 선택해주세요"),
    NOTICE("공지사항", "공지사항을 검색하고 선택해주세요"),
    EXTERNAL_LINK("외부 링크", "외부 URL을 입력해주세요")
}

enum class BannerSelectorType(
    val title: String,
    val searchPlaceholder: String
) {
    CAFE("운영 카페 선택", "운영 카페 이름을 검색해주세요"),
    NOTICE("공지사항 선택", "공지 제목을 검색해주세요"),
    EVENT("이벤트 선택", "이벤트 제목을 검색해주세요")
}
