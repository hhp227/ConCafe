package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.PendingCastClaimPreview

data class CafeDashboardUiState(
    val cafe: CafeDashboardData? = null,
    val castPreviews: List<CafeCastPreview> = emptyList(),
    val pendingCastClaims: List<PendingCastClaimPreview> = emptyList(),
    val selectedCastId: String? = null,
    val nextCastCursor: String? = null,
    val hasMoreCasts: Boolean = false,
    val isLoadingMoreCasts: Boolean = false,
    val isDeleteCastDialogVisible: Boolean = false,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
)
