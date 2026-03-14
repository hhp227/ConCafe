package com.hhp227.concafe.presentation.banner

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
    val isSaving: Boolean = false,
    val infoMessage: String? = "현재 활성화된 배너 슬롯이 가득 찬 경우, 등록된 배너는 예약 상태(SCHEDULED)로 대기하며 기존 배너 종료 시 자동으로 노출됩니다."
) {
    val displayDaysLabel: String
        get() = "${displayDays}일"

    val targetFieldPlaceholder: String
        get() = selectedTarget.placeholder

    val isSaveEnabled: Boolean
        get() = title.isNotBlank() &&
            subtitle.isNotBlank() &&
            targetValue.isNotBlank() &&
            !isSaving
}

enum class BannerTargetType(
    val label: String,
    val placeholder: String
) {
    CAFE_DETAIL("카페 상세", "대상 카페 ID를 입력해주세요"),
    EVENT_DETAIL("이벤트 상세", "대상 이벤트 ID를 입력해주세요"),
    NOTICE("공지사항", "대상 공지 ID를 입력해주세요"),
    EXTERNAL_LINK("외부 링크", "외부 URL을 입력해주세요")
}
