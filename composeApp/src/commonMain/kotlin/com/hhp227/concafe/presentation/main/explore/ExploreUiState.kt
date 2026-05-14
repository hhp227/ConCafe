package com.hhp227.concafe.presentation.main.explore

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.Cast

data class ExploreUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoginPromptVisible: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    val selectedTab: TabType = TabType.CAFE,
    val selectedRegion: RegionFilter = RegionFilter.ALL,
    val selectedSort: SortFilter = SortFilter.POPULAR,
    val cafes: List<Cafe> = emptyList(),
    val hasLoadedCafes: Boolean = false,
    val cafesNextCursor: String? = null,
    val canLoadMoreCafes: Boolean = false,
    val isLoadingMoreCafes: Boolean = false,
    val maids: List<Cast> = emptyList(),
    val hasLoadedMaids: Boolean = false,
    val maidsNextCursor: String? = null,
    val canLoadMoreMaids: Boolean = false,
    val isLoadingMoreMaids: Boolean = false
) {
    enum class TabType(val label: String) {
        CAFE("카페"),
        MAID("캐스트")
    }

    enum class RegionFilter(val label: String, val key: String) {
        ALL("전체", "all"),
        SEOUL("서울", "seoul"),
        BUSAN("부산", "busan"),
        DAEGU("대구", "daegu"),
        TOKYO("도쿄", "tokyo"),
        OSAKA("오사카", "osaka"),
        ETC("그 외지역", "etc")
    }

    enum class SortFilter(val label: String, val key: String) {
        POPULAR("인기순", "popular"),
        LATEST("최신순", "latest"),
        RATING("평점순", "rating")
    }

    companion object {
        fun empty() = ExploreUiState()
    }
}
