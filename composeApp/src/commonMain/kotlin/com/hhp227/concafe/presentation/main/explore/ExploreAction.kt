package com.hhp227.concafe.presentation.main.explore

sealed interface ExploreAction {
    data class QueryChanged(val query: String) : ExploreAction
    data class RegionChanged(val region: ExploreUiState.RegionFilter) : ExploreAction
    data class SortChanged(val sort: ExploreUiState.SortFilter) : ExploreAction
    data class TabChanged(val tab: ExploreUiState.TabType) : ExploreAction
    data class ClickCafe(val id: String) : ExploreAction
    data class ClickMaid(val id: String) : ExploreAction
    data object LoadMoreCafes : ExploreAction
    data object LoadMoreMaids : ExploreAction
    data object Refresh : ExploreAction
}
