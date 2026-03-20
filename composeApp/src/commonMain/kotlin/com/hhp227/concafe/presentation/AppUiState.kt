package com.hhp227.concafe.presentation

import com.hhp227.concafe.domain.model.NetworkAlertState

data class AppUiState(
    val networkAlertState: NetworkAlertState = NetworkAlertState.hidden
)
