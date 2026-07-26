package com.hhp227.concafe.presentation.main.cafemanagement.banner

data class BannerUiState(
    val selectedTab: BannerTab = BannerTab.ACTIVE,
    val banners: List<BannerItem> = emptyList(),
    val pendingDeleteBannerId: String? = null
) {
    val filteredBanners: List<BannerItem>
        get() = banners.filter { it.tab == selectedTab }

    val pendingDeleteBanner: BannerItem?
        get() = banners.firstOrNull { it.id == pendingDeleteBannerId }

    companion object {
        fun empty() = BannerUiState()
    }
}

data class BannerItem(
    val id: String,
    val cafeId: String?,
    val title: String,
    val description: String,
    val periodDays: Int,
    val statusLabelKey: String,
    val tab: BannerTab,
    val accentColorHex: String,
    val imageIcon: String,
    val imageUrl: String? = null
)

enum class BannerTab(val labelKey: String) {
    ACTIVE("banner_section_active"),
    SCHEDULED("banner_section_scheduled"),
    ENDED("banner_section_ended")
}
