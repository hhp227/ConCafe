package com.hhp227.concafe.presentation.settings.inquiry

sealed interface InquiryLinkEvent {
    data object NavigateBack : InquiryLinkEvent
    data class ShowMessage(val message: String) : InquiryLinkEvent
}
