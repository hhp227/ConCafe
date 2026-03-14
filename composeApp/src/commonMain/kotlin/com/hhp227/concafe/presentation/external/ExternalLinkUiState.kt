package com.hhp227.concafe.presentation.external

data class ExternalLinkUiState(
    val title: String,
    val url: String
) {
    val displayTitle: String
        get() = title.ifBlank { "외부 링크" }
}
