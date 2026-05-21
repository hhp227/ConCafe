package com.hhp227.concafe.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CommunityPostEvent
import com.hhp227.concafe.domain.event.publisher.CommunityPostEventPublisher
import com.hhp227.concafe.domain.usecase.GetCommunityPostPageUseCase
import com.hhp227.concafe.domain.usecase.LoadNativeAdUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val getCommunityPostPageUseCase: GetCommunityPostPageUseCase,
    private val communityPostEventPublisher: CommunityPostEventPublisher,
    private val loadNativeAdUseCase: LoadNativeAdUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CommunityEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun refresh() {
        _uiState.update { it.copy(isLoading = true, posts = emptyList(), nextCursor = null, hasNext = false, errorMessage = null, nativeAds = emptyMap()) }
        viewModelScope.launch {
            when (val result = getCommunityPostPageUseCase(cursor = null)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        posts = result.data.items,
                        nextCursor = result.data.nextCursor,
                        hasNext = result.data.hasNext
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "게시글을 불러오지 못했습니다.")
                }
            }
            if (_uiState.value.posts.isNotEmpty()) {
                loadNativeAd(pageIndex = 0)
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (!state.hasNext || state.isLoadingMore) return
        val pageIndex = state.posts.size / COMMUNITY_PAGE_SIZE
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            when (val result = getCommunityPostPageUseCase(cursor = state.nextCursor)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        posts = it.posts + result.data.items,
                        nextCursor = result.data.nextCursor,
                        hasNext = result.data.hasNext
                    )
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoadingMore = false) }
            }
            if (_uiState.value.posts.size > state.posts.size) {
                loadNativeAd(pageIndex = pageIndex)
            }
        }
    }

    private fun loadNativeAd(pageIndex: Int) {
        val slot = COMMUNITY_NATIVE_AD_SLOT_START + pageIndex
        if (_uiState.value.nativeAds.containsKey(slot)) return
        viewModelScope.launch {
            val ad = loadNativeAdUseCase.invoke(slot)
            _uiState.update { state ->
                state.copy(nativeAds = state.nativeAds + (slot to ad))
            }
        }
    }

    private fun observeCommunityPostEvent() {
        viewModelScope.launch {
            communityPostEventPublisher.events.collect { event ->
                when (event) {
                    is CommunityPostEvent.Created -> refresh()
                    is CommunityPostEvent.Deleted -> _uiState.update { state ->
                        state.copy(posts = state.posts.filter { it.id != event.postId })
                    }
                    is CommunityPostEvent.Updated -> _uiState.update { state ->
                        state.copy(posts = state.posts.map { if (it.id == event.post.id) event.post else it })
                    }
                }
            }
        }
    }

    fun onAction(action: CommunityAction) {
        when (action) {
            CommunityAction.Refresh -> refresh()
            CommunityAction.LoadMore -> loadMore()
            CommunityAction.ClickWritePost -> viewModelScope.launch {
                _event.emit(CommunityEvent.NavigateToPostEdit)
            }
            is CommunityAction.ClickPost -> viewModelScope.launch {
                _event.emit(CommunityEvent.NavigateToPost(action.postId))
            }
            CommunityAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    init {
        observeCommunityPostEvent()
        refresh()
    }

    private companion object {
        const val COMMUNITY_PAGE_SIZE = 20
        const val COMMUNITY_NATIVE_AD_SLOT_START = 100
    }
}
