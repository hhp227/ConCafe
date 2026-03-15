package com.hhp227.concafe.presentation.main.fanmanagement

import com.hhp227.concafe.domain.model.FanManagementData

data class FanManagementUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fanManagementData: FanManagementData? = null,
    val castClaimStatus: CastClaimStatusCard? = null,
    val castClaimSheet: CastClaimSheet? = null,
    val isClaimSheetVisible: Boolean = false,
    val stats: List<StatCard> = emptyList(),
    val recentFollowers: List<RecentFollower> = emptyList(),
    val topFans: List<TopFan> = emptyList(),
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
        val requestableCasts: List<ClaimCandidate> = emptyList(),
        val nextCursor: String? = null,
        val canLoadMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val selectedCastId: String? = null,
        val canSubmit: Boolean = false,
        val isSubmitting: Boolean = false
    )

    data class ClaimCandidate(
        val id: String,
        val name: String
    )

    data class StatCard(
        val label: String,
        val value: String,
        val highlight: Highlight
    )

    data class RecentFollower(
        val id: String,
        val name: String,
        val joinedLabel: String,
        val accent: Boolean = false
    ) {
        val initial: String
            get() = name.take(1).uppercase()
    }

    data class TopFan(
        val id: String,
        val rank: Int,
        val name: String,
        val pointsLabel: String,
        val isBest: Boolean = false
    )

    enum class QuickAction(
        val title: String,
        val subtitle: String
    ) {
        WORK_SCHEDULE("출근 관리", "이번 주 스케줄을 조정합니다."),
        CAFE_DASHBOARD("프로필 연결", "내 캐스트 프로필 연결 상태를 관리합니다.")
    }

    enum class Highlight {
        DEFAULT,
        PRIMARY
    }

    enum class Accent {
        PENDING,
        LINKED,
        REJECTED
    }

    companion object {
        fun empty(): FanManagementUiState = FanManagementUiState()
    }
}
