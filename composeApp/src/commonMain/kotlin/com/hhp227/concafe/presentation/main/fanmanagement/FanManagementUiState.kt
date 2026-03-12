package com.hhp227.concafe.presentation.main.fanmanagement

import com.hhp227.concafe.domain.model.FanManagementData

data class FanManagementUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fanManagementData: FanManagementData? = null,
    val stats: List<StatCard> = emptyList(),
    val recentFollowers: List<RecentFollower> = emptyList(),
    val topFans: List<TopFan> = emptyList(),
    val infoMessage: String? = null
) {
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
        WORK_SCHEDULE("출근 관리", "이번 주 스케줄을 조정합니다.")
    }

    enum class Highlight {
        DEFAULT,
        PRIMARY
    }

    companion object {
        fun empty(): FanManagementUiState = FanManagementUiState()
    }
}
