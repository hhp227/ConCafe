package com.hhp227.concafe.presentation.banner

sealed interface BannerEditEvent {
    data object NavigateBack : BannerEditEvent
    data object ShowSaveSuccessMessage : BannerEditEvent
}
