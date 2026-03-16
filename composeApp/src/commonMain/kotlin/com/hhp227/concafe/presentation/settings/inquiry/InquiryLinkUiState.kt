package com.hhp227.concafe.presentation.settings.inquiry

data class InquiryLinkUiState(
    val inquiryType: InquiryType = InquiryType.SERVICE,
    val title: String = "",
    val message: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun empty(): InquiryLinkUiState = InquiryLinkUiState()
    }
}

enum class InquiryType(val title: String) {
    SERVICE("고객지원"),
    BUG_REPORT("오류 제보"),
    SUGGESTION("서비스 제안")
}
