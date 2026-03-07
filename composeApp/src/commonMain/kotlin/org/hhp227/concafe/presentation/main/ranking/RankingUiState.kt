package org.hhp227.concafe.presentation.main.ranking

data class RankingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: TabType = TabType.MAIDS,
    val selectedPeriod: PeriodFilter = PeriodFilter.WEEKLY,
    val selectedRegion: RegionFilter = RegionFilter.ALL,
    val selectedAdIndex: Int = 0,
    val ads: List<PromoAd> = emptyList(),
    val maidRankings: List<RankingEntry> = emptyList(),
    val cafeRankings: List<RankingEntry> = emptyList()
) {
    enum class TabType(val label: String) {
        MAIDS("메이드 랭킹"),
        CAFES("카페 랭킹")
    }

    enum class PeriodFilter(val label: String) {
        WEEKLY("주간"),
        MONTHLY("월간")
    }

    enum class RegionFilter(val label: String) {
        ALL("전체"),
        SEOUL("서울"),
        TOKYO("도쿄"),
        OSAKA("오사카")
    }

    data class PromoAd(
        val id: String,
        val badge: String,
        val title: String,
        val subtitle: String,
        val description: String,
        val startColorHex: String,
        val endColorHex: String,
        val symbol: String
    )

    data class RankingEntry(
        val id: String,
        val rank: Int,
        val name: String,
        val subtitle: String,
        val score: Int,
        val change: String,
        val startColorHex: String,
        val endColorHex: String,
        val symbol: String
    )

    val currentAd: PromoAd
        get() = ads.getOrElse(selectedAdIndex.coerceIn(0, (ads.size - 1).coerceAtLeast(0))) {
            PromoAd("", "", "", "", "", "F6A8C5", "FFC8A2", "🎀")
        }

    val rankingEntries: List<RankingEntry>
        get() = if (selectedTab == TabType.MAIDS) maidRankings else cafeRankings

    companion object {
        val empty = RankingUiState()
    }
}
