package com.hhp227.concafe.presentation.castedit

data class CastEditUiState(
    val galleryMaxCount: Int = 3,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val screenTitle: String = "캐스트 프로필 수정",
    val saveButtonLabel: String = "프로필 저장",
    val profileImageUrl: String? = null,
    val castName: String = "",
    val conceptRole: String = "",
    val birthday: String = "",
    val introduction: String = "",
    val selectedWorkingDays: Set<WorkingDay> = emptySet(),
    val galleryImages: List<String> = emptyList(),
    val isImageRequiredAlertVisible: Boolean = false,
    val infoMessage: String? = null
) {
    val galleryLimitText: String
        get() = "${galleryImages.size} / $galleryMaxCount"

    enum class WorkingDay(val shortLabel: String) {
        MONDAY("Mon"),
        TUESDAY("Tue"),
        WEDNESDAY("Wed"),
        THURSDAY("Thu"),
        FRIDAY("Fri"),
        SATURDAY("Sat"),
        SUNDAY("Sun")
    }
}
