package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

interface ImagePrefetcher {
    fun prefetch(imageUrls: List<String?>, displaySize: ImageDisplaySize = ImageDisplaySize.THUMBNAIL)
}

@Composable
expect fun rememberImagePrefetcher(): ImagePrefetcher

@Composable
fun LazyListImagePrefetch(
    state: LazyListState,
    imageUrls: List<String?>,
    aheadCount: Int = 8,
    displaySize: ImageDisplaySize = ImageDisplaySize.THUMBNAIL
) {
    val prefetcher = rememberImagePrefetcher()
    val stableUrls = remember(imageUrls) { imageUrls.toList() }

    LaunchedEffect(state, stableUrls, aheadCount, displaySize, prefetcher) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex < 0) return@collectLatest
                prefetcher.prefetch(
                    imageUrls = stableUrls
                        .asSequence()
                        .drop(lastVisibleIndex + 1)
                        .take(aheadCount)
                        .toList(),
                    displaySize = displaySize
                )
            }
    }
}

@Composable
fun LazyGridImagePrefetch(
    state: LazyGridState,
    imageUrls: List<String?>,
    aheadCount: Int = 12,
    displaySize: ImageDisplaySize = ImageDisplaySize.THUMBNAIL
) {
    val prefetcher = rememberImagePrefetcher()
    val stableUrls = remember(imageUrls) { imageUrls.toList() }

    LaunchedEffect(state, stableUrls, aheadCount, displaySize, prefetcher) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1 }
            .distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex < 0) return@collectLatest
                prefetcher.prefetch(
                    imageUrls = stableUrls
                        .asSequence()
                        .drop(lastVisibleIndex + 1)
                        .take(aheadCount)
                        .toList(),
                    displaySize = displaySize
                )
            }
    }
}
