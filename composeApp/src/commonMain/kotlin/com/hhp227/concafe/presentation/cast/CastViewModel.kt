package com.hhp227.concafe.presentation.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase

class CastViewModel(
    private val castId: String,
    private val getCastDetailUseCase: GetCastDetailUseCase,
    private val toggleFollowCastUseCase: ToggleFollowCastUseCase,
    private val castEventPublisher: CastEventPublisher,
    private val reviewEventPublisher: ReviewEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(CastUiState.empty())
    val uiState: StateFlow<CastUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CastEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun observeCastEvent() {
        viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cast.id == castId) {
                        loadCastDetail()
                    }
                    is CastDomainEvent.Updated -> if (event.cast.id == castId) {
                        _uiState.update { state ->
                            state.copy(detail = state.detail?.copy(cast = event.cast))
                        }
                    }
                    is CastDomainEvent.Deleted -> if (event.castId == castId) {
                        _event.emit(CastEvent.NavigateBack)
                    }
                }
            }
        }
    }

    private fun observeReviewEvent() {
        viewModelScope.launch {
            reviewEventPublisher.events.collectLatest { event ->
                val currentCafeId = _uiState.value.detail?.cafe?.id ?: return@collectLatest
                when (event) {
                    is ReviewEvent.Created -> if (event.cafeId == currentCafeId) {
                        loadCastDetail()
                    }
                    is ReviewEvent.Deleted -> if (event.cafeId == currentCafeId) {
                        loadCastDetail()
                    }
                }
            }
        }
    }

    private fun loadCastDetail() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = getCastDetailUseCase.invoke(castId)) {
                is AppResult.Success -> {
                    _uiState.value = CastUiState(
                        isLoading = false,
                        errorMessage = null,
                        detail = result.data.detail,
                        recentReviews = result.data.recentReviews,
                        isFollowing = result.data.isFollowing,
                        isLoggedIn = result.data.isLoggedIn,
                        todayAttendanceStatus = result.data.todayAttendanceStatus,
                        isSelfCast = result.data.isSelfCast
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "캐스트 상세 데이터를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun toggleFollow() {
        viewModelScope.launch {
            when (val result = toggleFollowCastUseCase.invoke(castId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFollowing = result.data,
                            isLoggedIn = true
                        )
                    }
                }
                is AppResult.Failure -> {
                    if (result.error is AppError.Unauthorized) {
                        _event.emit(CastEvent.NavigateToSignIn)
                    }
                }
            }
        }
    }

    fun onAction(action: CastAction) {
        viewModelScope.launch {
            when (action) {
                CastAction.ClickBack -> _event.emit(CastEvent.NavigateBack)
                CastAction.ClickFollow -> toggleFollow()
                CastAction.Refresh -> loadCastDetail()
                CastAction.ClickCafe -> {
                    val cafeId = _uiState.value.detail?.cafe?.id ?: return@launch
                    _event.emit(CastEvent.NavigateToCafe(cafeId))
                }
            }
        }
    }

    init {
        observeCastEvent()
        observeReviewEvent()
        loadCastDetail()
    }
}
