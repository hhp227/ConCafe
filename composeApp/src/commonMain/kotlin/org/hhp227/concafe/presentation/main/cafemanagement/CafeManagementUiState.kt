package com.hhp227.concafe.presentation.main.cafemanagement

import com.hhp227.concafe.domain.model.CafeManagementData

data class CafeManagementUiState(
    val ownedCafes: List<CafeManagementData.OwnedCafeSummary> = emptyList(),
    val searchableCafes: List<CafeManagementData.SearchableCafeSummary> = emptyList(),
    val pendingClaims: List<CafeManagementData.PendingClaimSummary> = emptyList(),
    val isShowingAllCafes: Boolean = false,
    val cafeSearchQuery: String = "",
    val infoMessage: String? = null
) {
    val featuredCafe: CafeManagementData.OwnedCafeSummary?
        get() = ownedCafes.firstOrNull()

    val hasOwnedCafes: Boolean
        get() = ownedCafes.isNotEmpty()

    val visibleOwnedCafes: List<CafeManagementData.OwnedCafeSummary>
        get() = if (isShowingAllCafes) ownedCafes else ownedCafes.take(DEFAULT_VISIBLE_CAFE_COUNT)

    val hasHiddenOwnedCafes: Boolean
        get() = ownedCafes.size > DEFAULT_VISIBLE_CAFE_COUNT

    val filteredSearchableCafes: List<CafeManagementData.SearchableCafeSummary>
        get() = searchableCafes.filter {
            cafeSearchQuery.isBlank() ||
                it.name.contains(cafeSearchQuery, ignoreCase = true) ||
                it.location.contains(cafeSearchQuery, ignoreCase = true)
        }

    companion object {
        private const val DEFAULT_VISIBLE_CAFE_COUNT = 2
    }
}
