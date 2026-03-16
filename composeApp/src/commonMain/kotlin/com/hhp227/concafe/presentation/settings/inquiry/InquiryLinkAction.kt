package com.hhp227.concafe.presentation.settings.inquiry

sealed interface InquiryLinkAction {
    data object ClickBack : InquiryLinkAction
    data class ChangeInquiryType(val value: InquiryType) : InquiryLinkAction
    data class ChangeTitle(val value: String) : InquiryLinkAction
    data class ChangeMessage(val value: String) : InquiryLinkAction
    data object ClickSubmit : InquiryLinkAction
}
