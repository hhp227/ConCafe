package com.hhp227.concafe.presentation.main.community

import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.domain.model.CommunityPost

data class CommunityUiState(
    val isLoading: Boolean = false,
    val posts: List<CommunityPost> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val nativeAds: Map<Int, NativeAdHandle?> = emptyMap()
)
