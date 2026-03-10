package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import org.hhp227.concafe.domain.model.CafeDashboardData

data class CafeDashboardUiState(
    val cafe: CafeDashboardData? = null,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
)
