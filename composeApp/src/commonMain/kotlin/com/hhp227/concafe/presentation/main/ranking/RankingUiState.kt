package com.hhp227.concafe.presentation.main.ranking

import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.domain.model.RankingFeedEntry
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.RankingPromoAd

data class RankingUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoginPromptVisible: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: TabType = TabType.MAIDS,
    val selectedPeriod: RankingPeriod = RankingPeriod.WEEKLY,
    val selectedRegion: RegionFilter = RegionFilter.ALL,
    val selectedAdIndex: Int = 0,
    val ads: List<RankingPromoAd> = emptyList(),
    val maidRankings: List<RankingFeedEntry> = emptyList(),
    val cafeRankings: List<RankingFeedEntry> = emptyList(),
    val bannerHeightPx: Int = 0,
    val nativeAdSlot1: NativeAdHandle? = null,
    val nativeAdSlot2: NativeAdHandle? = null
) {
    enum class TabType(val label: String) {
        MAIDS("캐스트 랭킹"),
        CAFES("카페 랭킹")
    }

    enum class RegionFilter(val label: String, val country: String?, val city: String?) {
        ALL("전체", null, null),
        SEOUL("서울", "KR", "Seoul"),
        BUSAN("부산", "KR", "Busan"),
        DAEGU("대구", "KR", "Daegu"),
        TOKYO("도쿄", "JP", "Tokyo"),
        OSAKA("오사카", "JP", "Osaka"),
        YOKOHAMA("요코하마", "JP", "Yokohama")
    }

    val currentAd: RankingPromoAd
        get() = ads.getOrElse(selectedAdIndex.coerceIn(0, (ads.size - 1).coerceAtLeast(0))) {
            RankingPromoAd("", "", "", "", "", "F6A8C5", "FFC8A2", "🎀")
        }

    val rankingEntries: List<RankingFeedEntry>
        get() = if (selectedTab == TabType.MAIDS) maidRankings else cafeRankings

    companion object {
        val empty = RankingUiState()
    }
}
