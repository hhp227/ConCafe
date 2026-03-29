package com.hhp227.concafe.presentation.main.fanmanagement

import com.hhp227.concafe.domain.model.FanManagementData
import com.hhp227.concafe.domain.model.CastClaimCandidate

data class FanManagementUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fanManagementData: FanManagementData? = null,
    val castClaimStatus: CastClaimStatusCard? = null,
    val castClaimSheet: CastClaimSheet? = null,
    val isClaimSheetVisible: Boolean = false,
    val isAnnouncementSheetVisible: Boolean = false,
    val announcementTitle: String = "",
    val announcementBody: String = "",
    val isSendingAnnouncement: Boolean = false,
    val infoMessage: String? = null
) {
    data class CastClaimStatusCard(
        val affiliatedCafeId: String,
        val affiliatedCafeName: String,
        val headline: String,
        val body: String,
        val accent: Accent
    )

    data class CastClaimSheet(
        val affiliatedCafeId: String,
        val affiliatedCafeName: String,
        val headline: String,
        val body: String,
        val requestableCasts: List<CastClaimCandidate> = emptyList(),
        val nextCursor: String? = null,
        val canLoadMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val selectedCastId: String? = null,
        val canSubmit: Boolean = false,
        val isSubmitting: Boolean = false
    )

    enum class QuickAction(
        val title: String,
        val subtitle: String
    ) {
        WORK_SCHEDULE("출근 관리", "이번 주 스케줄을 조정합니다."),
        CAFE_DASHBOARD("프로필 연결", "내 캐스트 프로필 연결 상태를 관리합니다.")
    }

    enum class Accent {
        PENDING,
        LINKED,
        REJECTED
    }

    val isAnnouncementSubmitEnabled: Boolean
        get() = announcementTitle.trim().isNotEmpty() &&
            announcementBody.trim().isNotEmpty() &&
            !isSendingAnnouncement

    companion object {
        fun empty(): FanManagementUiState = FanManagementUiState()
    }
}
