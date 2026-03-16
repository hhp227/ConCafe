package com.hhp227.concafe.presentation.main.admin

sealed interface AdminOperationsEvent {
    data object NavigateToBannerEdit : AdminOperationsEvent
}
