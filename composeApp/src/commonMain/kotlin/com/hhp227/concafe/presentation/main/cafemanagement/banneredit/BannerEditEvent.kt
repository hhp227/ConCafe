package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

sealed interface BannerEditEvent {
    data object NavigateBack : BannerEditEvent
    data object ShowSaveSuccessMessage : BannerEditEvent
}
