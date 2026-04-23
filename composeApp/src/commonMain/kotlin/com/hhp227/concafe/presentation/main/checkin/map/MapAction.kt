package com.hhp227.concafe.presentation.main.checkin.map

import com.hhp227.concafe.presentation.main.explore.ExploreUiState

sealed interface MapAction {
    data object ClickBack : MapAction

    data class ClickCafe(val id: String) : MapAction

    data class UpdateRegion(val region: ExploreUiState.RegionFilter) : MapAction

    data class UpdateSearchQuery(val query: String) : MapAction
}
