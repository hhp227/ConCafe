package com.hhp227.concafe.presentation.main.community.edit

data class PostEditUiState(
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val imageMaxCount: Int = 5,
    val isEditMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val infoMessage: String? = null
) {
    val canSubmit: Boolean get() = title.isNotBlank() && content.isNotBlank()
}
