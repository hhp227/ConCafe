package com.hhp227.concafe.presentation.main.admin

import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.Report

data class AdminOperationsUiState(
    val totalUsersCount: Int = 0,
    val activeCafesCount: Int = 0,
    val reportItemsCount: Int = 0,
    val metrics: List<AdminMetricCard> = buildAdminMetrics(
        totalUsersCount = 0,
        activeCafesCount = 0,
        pendingCount = 0,
        reportItemsCount = 0
    ),
    val selectedPendingFilter: PendingFilter = PendingFilter.CAFE_REGISTRATION,
    val pendingCafeRegistrationClaims: List<PendingCafeRegistrationClaimPreview> = emptyList(),
    val pendingCafeOwnerClaims: List<PendingCafeOwnerClaimPreview> = emptyList(),
    val inquiries: List<Inquiry> = emptyList(),
    val reports: List<Report> = emptyList(),
    val inquiryNextCursor: String? = null,
    val reportNextCursor: String? = null,
    val canLoadMoreInquiries: Boolean = false,
    val canLoadMoreReports: Boolean = false,
    val isLoadingMoreInquiries: Boolean = false,
    val isLoadingMoreReports: Boolean = false,
    val quickMenus: List<AdminQuickMenu> = defaultQuickMenus,
    val hasUnreadNotifications: Boolean = true,
    val infoMessage: String? = null
) {
    val pendingFilters: List<PendingFilterChip>
        get() = PendingFilter.entries.map { filter ->
            PendingFilterChip(
                filter = filter,
                label = filter.label,
                count = when (filter) {
                    PendingFilter.CAFE_REGISTRATION -> pendingCafeRegistrationClaims.size
                    PendingFilter.ROLE_CLAIM -> pendingCafeOwnerClaims.size
                },
                isSelected = selectedPendingFilter == filter
            )
        }
}

data class AdminMetricCard(
    val title: String,
    val value: String,
    val delta: String,
    val icon: AdminMetricIcon,
    val trend: MetricTrend
)

data class PendingFilterChip(
    val filter: PendingFilter,
    val label: String,
    val count: Int,
    val isSelected: Boolean
)

data class AdminQuickMenu(
    val id: String,
    val title: String,
    val description: String,
    val icon: QuickMenuIcon,
    val accent: QuickMenuAccent
)

enum class PendingFilter(val label: String) {
    CAFE_REGISTRATION("카페 등록"),
    ROLE_CLAIM("권한 신청")
}

enum class MetricTrend {
    UP,
    DOWN,
    NEW
}

enum class AdminMetricIcon {
    USERS,
    CAFE,
    PENDING,
    REPORT
}

enum class QuickMenuIcon {
    USERS,
    BANNER,
    MODERATION,
    ANALYTICS,
    DORMANT
}

enum class QuickMenuAccent {
    PRIMARY,
    ROSE,
    BLUE
}

internal fun buildAdminMetrics(
    totalUsersCount: Int,
    activeCafesCount: Int,
    pendingCount: Int,
    reportItemsCount: Int
) = listOf(
    AdminMetricCard("전체 사용자", totalUsersCount.toString(), "실시간", AdminMetricIcon.USERS, MetricTrend.UP),
    AdminMetricCard("활성 카페", activeCafesCount.toString(), "실시간", AdminMetricIcon.CAFE, MetricTrend.UP),
    AdminMetricCard("승인 대기", pendingCount.toString(), "${pendingCount}건 대기", AdminMetricIcon.PENDING, MetricTrend.NEW),
    AdminMetricCard("신고 항목", reportItemsCount.toString(), "실시간", AdminMetricIcon.REPORT, MetricTrend.DOWN)
)

private val defaultQuickMenus = listOf(
    AdminQuickMenu("users", "유저 관리", "카페 운영자 및 차단 사용자 조회", QuickMenuIcon.USERS, QuickMenuAccent.BLUE),
    AdminQuickMenu("dormant", "휴면계정 관리", "장기 미접속 및 휴면 계정 관리", QuickMenuIcon.DORMANT, QuickMenuAccent.ROSE),
    AdminQuickMenu("banner", "홈 배너 관리", "이벤트 및 공지 배너 수정", QuickMenuIcon.BANNER, QuickMenuAccent.PRIMARY),
    AdminQuickMenu("moderation", "신고 및 제재", "부적절한 컨텐츠 및 유저 차단", QuickMenuIcon.MODERATION, QuickMenuAccent.ROSE),
    AdminQuickMenu("analytics", "시스템 통계", "유입 분석 및 매출 리포트", QuickMenuIcon.ANALYTICS, QuickMenuAccent.BLUE)
)
