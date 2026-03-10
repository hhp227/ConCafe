package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

data class CafeDashboardUiState(
    val cafe: CafeSummary = sampleCafes().first(),
    val infoMessage: String? = null
) {
    data class CafeSummary(
        val id: String,
        val name: String,
        val city: String,
        val isApproved: Boolean,
        val todayVisitors: Int,
        val todayCheckIns: Int,
        val todayReviews: Int,
        val rating: Double,
        val castCount: Int,
        val noticeCount: Int,
        val eventCount: Int,
        val externalLinkCount: Int,
        val castPreviews: List<CastPreview>,
        val homeBannerPreview: HomeBannerPreview
    )

    data class CastPreview(
        val id: String,
        val name: String,
        val isOnShift: Boolean
    )

    data class HomeBannerPreview(
        val title: String,
        val period: String,
        val statusLabel: String
    )

    enum class Shortcut(
        val title: String,
        val subtitle: String
    ) {
        CAST_MANAGEMENT("캐스트 관리", "프로필, 사진, 팬 연결"),
        CAST_SCHEDULE("출근표", "오늘 출근과 주간 일정"),
        EVENT_MANAGEMENT("공지&이벤트", "공지 작성과 시즌 이벤트"),
        CAFE_SETTINGS("카페 정보 관리", "기본 정보와 소개 관리"),
        MENU_GOODS("메뉴&굿즈", "메뉴와 판매 굿즈 관리"),
        HOME_BANNER("홈 배너", "예약 배너와 진행 상태"),
        EXTERNAL_LINKS("외부 링크", "Instagram, X, TikTok")
    }

    companion object {
        fun preview(cafeId: String) = CafeDashboardUiState(
            cafe = sampleCafes().firstOrNull { it.id == cafeId } ?: sampleCafes().first()
        )

        private fun sampleCafes() = listOf(
            CafeSummary(
                id = "cafe-1",
                name = "Maid Dream Tokyo",
                city = "Tokyo",
                isApproved = true,
                todayVisitors = 18,
                todayCheckIns = 12,
                todayReviews = 3,
                rating = 4.7,
                castCount = 8,
                noticeCount = 2,
                eventCount = 2,
                externalLinkCount = 4,
                castPreviews = listOf(
                    CastPreview("cast-1", "미유", true),
                    CastPreview("cast-2", "하나", false),
                    CastPreview("cast-3", "리코", true)
                ),
                homeBannerPreview = HomeBannerPreview(
                    title = "여름 한정 신메뉴 출시!",
                    period = "2026.06.01 - 2026.08.31",
                    statusLabel = "노출 중"
                )
            ),
            CafeSummary(
                id = "cafe-2",
                name = "Seoul Maid Cafe",
                city = "Seoul",
                isApproved = true,
                todayVisitors = 11,
                todayCheckIns = 7,
                todayReviews = 1,
                rating = 4.5,
                castCount = 5,
                noticeCount = 1,
                eventCount = 1,
                externalLinkCount = 3,
                castPreviews = listOf(
                    CastPreview("cast-4", "사나", true),
                    CastPreview("cast-5", "유리", true),
                    CastPreview("cast-6", "노아", false)
                ),
                homeBannerPreview = HomeBannerPreview(
                    title = "주말 콜라보 디저트 오픈",
                    period = "2026.03.14 - 2026.03.31",
                    statusLabel = "예약 중"
                )
            ),
            CafeSummary(
                id = "cafe-3",
                name = "Akihabara Butler Cafe",
                city = "Tokyo",
                isApproved = false,
                todayVisitors = 0,
                todayCheckIns = 0,
                todayReviews = 0,
                rating = 0.0,
                castCount = 3,
                noticeCount = 0,
                eventCount = 0,
                externalLinkCount = 1,
                castPreviews = listOf(
                    CastPreview("cast-7", "레이", false),
                    CastPreview("cast-8", "카렌", false)
                ),
                homeBannerPreview = HomeBannerPreview(
                    title = "신규 오픈 안내 배너",
                    period = "2026.03.20 - 2026.04.20",
                    statusLabel = "검수 중"
                )
            )
        )
    }
}
