package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import org.hhp227.concafe.domain.model.CafeCastPreview
import org.hhp227.concafe.domain.model.CafeDashboardData

data class CafeDashboardUiState(
    val cafe: CafeDashboardData? = null,
    val castPreviews: List<CafeCastPreview> = emptyList(),
    val nextCastCursor: String? = null,
    val hasMoreCasts: Boolean = false,
    val isLoadingMoreCasts: Boolean = false,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
)
