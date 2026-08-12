package com.hhp227.concafe.presentation.main.admin

sealed interface AdminOperationsEvent {
    data object NavigateToBanner : AdminOperationsEvent

    data object NavigateToBannerEdit : AdminOperationsEvent

    data object NavigateToUserManagement : AdminOperationsEvent

    data object NavigateToDormantAccount : AdminOperationsEvent
}
