package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

sealed interface BannerEditAction {
    data object ClickBack : BannerEditAction
    data object ClickImagePicker : BannerEditAction
    data class ChangeTitle(val value: String) : BannerEditAction
    data class ChangeSubtitle(val value: String) : BannerEditAction
    data class SelectTarget(val target: BannerTargetType) : BannerEditAction
    data class ChangeTargetValue(val value: String) : BannerEditAction
    data class ChangeDisplayDays(val value: Int) : BannerEditAction
    data object ClickCafeSelector : BannerEditAction
    data object ClickTargetSelector : BannerEditAction
    data class ChangeSelectorQuery(val value: String) : BannerEditAction
    data class SelectSelectorItem(val id: String) : BannerEditAction
    data object DismissSelector : BannerEditAction
    data object ClickSave : BannerEditAction
    data object DismissInfoMessage : BannerEditAction
}
