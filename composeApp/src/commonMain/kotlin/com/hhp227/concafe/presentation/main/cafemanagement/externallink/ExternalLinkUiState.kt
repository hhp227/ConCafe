package com.hhp227.concafe.presentation.main.cafemanagement.externallink

data class ExternalLinkUiState(
    val title: String,
    val url: String
) {
    val displayTitle: String
        get() = title.ifBlank { "외부 링크" }
}
