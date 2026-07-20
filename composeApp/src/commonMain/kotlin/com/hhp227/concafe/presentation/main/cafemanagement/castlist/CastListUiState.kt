package com.hhp227.concafe.presentation.main.cafemanagement.castlist

import com.hhp227.concafe.domain.model.CafeCastPreview

data class CastListUiState(
    val casts: List<CafeCastPreview> = emptyList(),
    val searchQuery: String = "",
    val deleteTargetCastId: String? = null,
    val nextCursor: String? = null,
    val hasMoreCasts: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
) {
    val filteredCasts: List<CafeCastPreview>
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                casts
            } else {
                casts.filter { it.name.contains(query, ignoreCase = true) }
            }
        }

    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val isDeleteCastDialogVisible: Boolean
        get() = deleteTargetCastId != null
}
