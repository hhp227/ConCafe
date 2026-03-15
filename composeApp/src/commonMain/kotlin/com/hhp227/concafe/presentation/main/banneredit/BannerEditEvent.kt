package com.hhp227.concafe.presentation.main.banneredit

sealed interface BannerEditEvent {
    data object NavigateBack : BannerEditEvent
    data object ShowSaveSuccessMessage : BannerEditEvent
}
