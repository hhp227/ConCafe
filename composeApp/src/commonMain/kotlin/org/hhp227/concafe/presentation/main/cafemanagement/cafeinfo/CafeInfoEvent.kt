package org.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

sealed interface CafeInfoEvent {
    data object NavigateBack : CafeInfoEvent
    data object ShowSaveSuccessMessage : CafeInfoEvent
}
