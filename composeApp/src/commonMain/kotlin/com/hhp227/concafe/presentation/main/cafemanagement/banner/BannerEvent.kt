package com.hhp227.concafe.presentation.main.cafemanagement.banner

sealed interface BannerEvent {
    data object NavigateBack : BannerEvent
    data class NavigateToBannerEdit(
        val cafeId: String?,
        val bannerId: String? = null
    ) : BannerEvent
    data class ShowMessage(val message: String) : BannerEvent
}
