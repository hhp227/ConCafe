package com.hhp227.concafe.presentation.castedit

data class CastEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val screenTitle: String = "캐스트 프로필 수정",
    val saveButtonLabel: String = "프로필 저장",
    val castName: String = "",
    val conceptRole: String = "",
    val birthday: String = "",
    val introduction: String = "",
    val selectedWorkingDays: Set<WorkingDay> = emptySet(),
    val galleryItems: List<GalleryItem> = emptyList(),
    val infoMessage: String? = null
) {
    enum class WorkingDay(val shortLabel: String) {
        MONDAY("Mon"),
        TUESDAY("Tue"),
        WEDNESDAY("Wed"),
        THURSDAY("Thu"),
        FRIDAY("Fri"),
        SATURDAY("Sat"),
        SUNDAY("Sun")
    }

    data class GalleryItem(
        val id: String,
        val label: String,
        val overlayCount: Int? = null
    )
}
