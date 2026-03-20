package com.hhp227.concafe.presentation.main.cafemanagement.banner

sealed interface BannerAction {
    data object ClickBack : BannerAction
    data class SelectTab(val tab: BannerTab) : BannerAction
    data object ClickCreateBanner : BannerAction
    data class ClickEditBanner(val bannerId: String) : BannerAction
    data class ClickDeleteBanner(val bannerId: String) : BannerAction
    data object ConfirmDeleteBanner : BannerAction
    data object DismissDeleteBannerDialog : BannerAction
}
