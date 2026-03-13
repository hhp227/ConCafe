package com.hhp227.concafe.presentation.main.admin

data class AdminOperationsUiState(
    val metrics: List<AdminMetricCard> = defaultAdminMetrics,
    val selectedPendingFilter: PendingFilter = PendingFilter.CAFE_REGISTRATION,
    val pendingRequests: List<AdminPendingRequest> = defaultPendingRequests,
    val quickMenus: List<AdminQuickMenu> = defaultQuickMenus,
    val hasUnreadNotifications: Boolean = true,
    val infoMessage: String? = null
) {
    val pendingFilters: List<PendingFilterChip>
        get() = PendingFilter.entries.map { filter ->
            PendingFilterChip(
                filter = filter,
                label = filter.label,
                count = pendingRequests.count { it.type == filter },
                isSelected = selectedPendingFilter == filter
            )
        }

    val filteredPendingRequests: List<AdminPendingRequest>
        get() = pendingRequests.filter { it.type == selectedPendingFilter }
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

data class AdminPendingRequest(
    val id: String,
    val type: PendingFilter,
    val title: String,
    val subtitle: String,
    val requestedAt: String,
    val imageUrl: String
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
    BANNER,
    MODERATION,
    ANALYTICS
}

enum class QuickMenuAccent {
    PRIMARY,
    ROSE,
    BLUE
}

private val defaultAdminMetrics = listOf(
    AdminMetricCard("전체 사용자", "12,540", "1.2%", AdminMetricIcon.USERS, MetricTrend.UP),
    AdminMetricCard("활성 카페", "842", "0.5%", AdminMetricIcon.CAFE, MetricTrend.UP),
    AdminMetricCard("승인 대기", "2", "2건 대기", AdminMetricIcon.PENDING, MetricTrend.NEW),
    AdminMetricCard("신고 항목", "32", "8%", AdminMetricIcon.REPORT, MetricTrend.DOWN)
)

private val defaultPendingRequests = listOf(
    AdminPendingRequest(
        id = "pending-cafe-1",
        type = PendingFilter.CAFE_REGISTRATION,
        title = "카페 모카라떼 홍대점",
        subtitle = "서울 마포구 어울마당로 123",
        requestedAt = "2시간 전",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAPTqu6TR5iE7rtn6cuSTaGUwIAdgNS9xaZqDyHkBXX25arxUP3ZAK6wS2HHUj-Efew3j9cuymLzCx7a7fUG8MqyZ1HFdgXXJoSTw9zIlWv0cvk_sjAIt-6daNAoEAg0lQTCaCkZ7CSKX2uNQpH9gyyUjrU2UdHrmBskzC9nIr06ms2YgAbzHhPdxZbEVZN41SPq6gUqSSTdRWJcI5AS-T3HTjq3n3yMJYZ7T_imgYTE1UrUdAnniws6bLwUzX_o9f7XcBOy5Ur9A"
    ),
    AdminPendingRequest(
        id = "pending-cafe-2",
        type = PendingFilter.CAFE_REGISTRATION,
        title = "디저트 빌리지 성수",
        subtitle = "서울 성동구 아차산로 45",
        requestedAt = "5시간 전",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCe9Vib6B40fIweAG4csR1KYxnHTMoec_xzj6GS5343QHIszmvCk4_ZiPt1NOdpLruSfby0tdpH2myNthY3GZjMgDZw8Fjh70hjE55AGaHkmkMJdLkqsuISq4Gsa8WhO-JRD3SIBIY_FAoBdHYRxqq2AVZl7Xmrgp0OorSTkcVTdF6cO14mBMWbvzhU9Hga3y41jSo89iuQ8aG-D8oKHX5PPyeXXGllTSzc7oGE8PMT1rBx-DRviiY0QI2H9AvdAbcm8hHiBGfIVQ"
    )
)

private val defaultQuickMenus = listOf(
    AdminQuickMenu("banner", "홈 배너 관리", "이벤트 및 공지 배너 수정", QuickMenuIcon.BANNER, QuickMenuAccent.PRIMARY),
    AdminQuickMenu("moderation", "신고 및 제재", "부적절한 컨텐츠 및 유저 차단", QuickMenuIcon.MODERATION, QuickMenuAccent.ROSE),
    AdminQuickMenu("analytics", "시스템 통계", "유입 분석 및 매출 리포트", QuickMenuIcon.ANALYTICS, QuickMenuAccent.BLUE)
)
