package com.hhp227.concafe.presentation.cafe.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeEventEvent as DomainCafeEventEvent
import com.hhp227.concafe.domain.event.publisher.CafeEventEventPublisher
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.usecase.GetCafeEventLikeStatusUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventParticipantCastsUseCase
import com.hhp227.concafe.domain.usecase.ToggleCafeEventLikeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CafeEventViewModel(
    private val cafeId: String,
    private val eventId: String,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase,
    private val cafeEventEventPublisher: CafeEventEventPublisher,
    private val getCafeEventLikeStatusUseCase: GetCafeEventLikeStatusUseCase,
    private val toggleCafeEventLikeUseCase: ToggleCafeEventLikeUseCase,
    private val getCafeEventParticipantCastsUseCase: GetCafeEventParticipantCastsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeEventUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeEventEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun loadEvent() {
        jobs[JobKey.LOAD_EVENT]?.cancel()
        jobs[JobKey.LOAD_EVENT] = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            var cursor: String? = null
            var selected: CafeEventManagementItem? = null
            var failed = false

            do {
                when (val result = getCafeEventPageUseCase.invoke(cafeId = cafeId, query = "", cursor = cursor, pageSize = EVENT_PAGE_SIZE)) {
                    is AppResult.Success -> {
                        selected = result.data.items.firstOrNull { it.id == eventId } ?: selected
                        cursor = result.data.nextCursor
                        if (selected != null || !result.data.hasNext) break
                    }
                    is AppResult.Failure -> {
                        failed = true
                        break
                    }
                }
            } while (cursor != null)

            if (failed) {
                _uiState.update { it.copy(isLoading = false, errorMessage = EVENT_LOAD_FAILED_MESSAGE) }
            } else if (selected != null) {
                val likeStatusDeferred = async {
                    (getCafeEventLikeStatusUseCase(cafeId, eventId) as? AppResult.Success)?.data ?: false
                }
                val castsDeferred = async {
                    (getCafeEventParticipantCastsUseCase(selected.participantCastIds) as? AppResult.Success)?.data.orEmpty()
                }
                val isLiked = likeStatusDeferred.await()
                val casts = castsDeferred.await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        event = selected,
                        likeCount = selected.likeCount,
                        isLikedByMe = isLiked,
                        participantCasts = casts,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, event = null, errorMessage = EVENT_NOT_FOUND_MESSAGE) }
            }
        }
    }

    private fun observeCafeEventEvent() {
        jobs[JobKey.OBSERVE_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_EVENT] = viewModelScope.launch {
            cafeEventEventPublisher.events.collectLatest { event ->
                when (event) {
                    is DomainCafeEventEvent.Created -> Unit
                    is DomainCafeEventEvent.Updated -> {
                        if (event.cafeId == cafeId && event.event.id == eventId) {
                            val updatedEvent = event.event
                            val casts = (getCafeEventParticipantCastsUseCase(updatedEvent.participantCastIds) as? AppResult.Success)?.data.orEmpty()
                            _uiState.update {
                                it.copy(
                                    event = updatedEvent,
                                    likeCount = updatedEvent.likeCount,
                                    participantCasts = casts,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                    is DomainCafeEventEvent.Deleted -> {
                        if (event.cafeId == cafeId && event.eventId == eventId) {
                            _uiState.update { it.copy(event = null, errorMessage = EVENT_NOT_FOUND_MESSAGE) }
                        }
                    }
                }
            }
        }
    }

    private fun toggleLike() {
        if (_uiState.value.isTogglingLike) return
        jobs[JobKey.TOGGLE_LIKE]?.cancel()
        jobs[JobKey.TOGGLE_LIKE] = viewModelScope.launch {
            val current = _uiState.value
            val optimisticLiked = !current.isLikedByMe
            val optimisticCount = if (optimisticLiked) current.likeCount + 1 else maxOf(0, current.likeCount - 1)
            _uiState.update { it.copy(isLikedByMe = optimisticLiked, likeCount = optimisticCount, isTogglingLike = true) }

            when (val result = toggleCafeEventLikeUseCase(cafeId, eventId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLikedByMe = result.data, isTogglingLike = false) }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLikedByMe = current.isLikedByMe, likeCount = current.likeCount, isTogglingLike = false) }
                    if (result.error is AppError.Unauthorized) {
                        _event.emit(CafeEventEvent.NavigateToSignIn)
                    }
                }
            }
        }
    }

    fun onAction(action: CafeEventAction) {
        viewModelScope.launch {
            when (action) {
                CafeEventAction.ClickBack -> _event.emit(CafeEventEvent.NavigateBack)
                CafeEventAction.Retry -> loadEvent()
                CafeEventAction.ToggleLike -> toggleLike()
                CafeEventAction.GoToCafe -> _event.emit(CafeEventEvent.NavigateToCafe)
                is CafeEventAction.ClickCast -> _event.emit(CafeEventEvent.NavigateToCast(action.castId))
            }
        }
    }

    init {
        observeCafeEventEvent()
        loadEvent()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class JobKey {
        LOAD_EVENT,
        OBSERVE_EVENT,
        TOGGLE_LIKE
    }

    private companion object {
        const val EVENT_PAGE_SIZE = 100
        const val EVENT_LOAD_FAILED_MESSAGE = "Failed to load event."
        const val EVENT_NOT_FOUND_MESSAGE = "Event not found."
    }
}
