package com.hhp227.concafe.presentation.main.cafemanagement.banner

data class BannerUiState(
    val screenTitle: String = "배너 관리",
    val selectedTab: BannerTab = BannerTab.ACTIVE,
    val locationLabel: String = "홈 화면 상단",
    val banners: List<BannerItem> = emptyList()
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
    val accentColorHex: String,
    val imageIcon: String
)

enum class BannerTab(val label: String) {
    ACTIVE("진행 중"),
    SCHEDULED("예약"),
    ENDED("종료")
}
