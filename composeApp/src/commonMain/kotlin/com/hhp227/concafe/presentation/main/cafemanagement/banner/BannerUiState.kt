package com.hhp227.concafe.presentation.main.cafemanagement.banner

data class BannerUiState(
    val screenTitle: String = "배너 관리",
    val selectedTab: BannerTab = BannerTab.ACTIVE,
    val locationLabel: String = "홈 화면 상단",
    val banners: List<BannerItem> = sampleBannerItems()
) {
    val filteredBanners: List<BannerItem>
        get() = banners.filter { it.tab == selectedTab }

    val sectionTitle: String
        get() = when (selectedTab) {
            BannerTab.ACTIVE -> "현재 노출 중인 배너"
            BannerTab.SCHEDULED -> "노출 예정 배너"
            BannerTab.ENDED -> "종료된 배너"
        }

    val sectionCountLabel: String
        get() = "${sectionTitle} (${filteredBanners.size})"

    companion object {
        fun empty() = BannerUiState()
    }
}

data class BannerItem(
    val id: String,
    val title: String,
    val description: String,
    val periodText: String,
    val statusLabel: String,
    val tab: BannerTab,
    val accentColor: Long,
    val imageIcon: String
)

enum class BannerTab(val label: String) {
    ACTIVE("진행 중"),
    SCHEDULED("예약"),
    ENDED("종료")
}

internal fun sampleBannerItems(): List<BannerItem> = listOf(
    BannerItem(
        id = "banner-active-1",
        title = "겨울 시즌 딸기 라떼 할인",
        description = "상큼한 딸기 메뉴 20% 할인 이벤트",
        periodText = "2023.12.01 ~ 2023.12.31",
        statusLabel = "진행 중",
        tab = BannerTab.ACTIVE,
        accentColor = 0xFFF27CA6,
        imageIcon = "local_cafe"
    ),
    BannerItem(
        id = "banner-active-2",
        title = "신규 멤버십 가입 혜택",
        description = "지금 가입하면 아메리카노 1잔 무료",
        periodText = "2023.11.15 ~ 2024.01.15",
        statusLabel = "진행 중",
        tab = BannerTab.ACTIVE,
        accentColor = 0xFFCE6A8C,
        imageIcon = "card_giftcard"
    ),
    BannerItem(
        id = "banner-active-3",
        title = "주말 특별 연주회 안내",
        description = "매주 토요일 오후 3시, 라이브 재즈",
        periodText = "2023.12.01 ~ 2023.12.31",
        statusLabel = "진행 중",
        tab = BannerTab.ACTIVE,
        accentColor = 0xFF8A5B73,
        imageIcon = "music_note"
    ),
    BannerItem(
        id = "banner-scheduled-1",
        title = "화이트데이 디저트 페어",
        description = "화이트 초콜릿 디저트 라인업 미리보기",
        periodText = "2024.03.10 ~ 2024.03.17",
        statusLabel = "예약",
        tab = BannerTab.SCHEDULED,
        accentColor = 0xFFF3AFC5,
        imageIcon = "cake"
    ),
    BannerItem(
        id = "banner-scheduled-2",
        title = "신메뉴 브런치 런칭",
        description = "주말 브런치 신메뉴를 곧 공개합니다",
        periodText = "2024.03.20 ~ 2024.04.20",
        statusLabel = "예약",
        tab = BannerTab.SCHEDULED,
        accentColor = 0xFFD78EA9,
        imageIcon = "restaurant"
    ),
    BannerItem(
        id = "banner-ended-1",
        title = "연말 감사 쿠폰 이벤트",
        description = "방문 고객 대상 감사 쿠폰 증정",
        periodText = "2023.12.20 ~ 2024.01.05",
        statusLabel = "종료",
        tab = BannerTab.ENDED,
        accentColor = 0xFFBDA3AE,
        imageIcon = "redeem"
    ),
    BannerItem(
        id = "banner-ended-2",
        title = "설 연휴 운영 안내",
        description = "설 연휴 영업시간과 휴무일 안내",
        periodText = "2024.02.07 ~ 2024.02.12",
        statusLabel = "종료",
        tab = BannerTab.ENDED,
        accentColor = 0xFF94808A,
        imageIcon = "event_note"
    )
)
