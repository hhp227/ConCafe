package com.hhp227.concafe.presentation.main.checkin.map

import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.presentation.main.explore.ExploreUiState

data class MapUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentLocationLabel: String = "",
    val userCityKey: String? = null,
    val mapCafes: List<CheckInCafeSummary> = emptyList(),
    val selectedRegion: ExploreUiState.RegionFilter = ExploreUiState.RegionFilter.ALL,
    val searchQuery: String = ""
) {
    companion object {
        fun empty() = MapUiState()
    }
}
