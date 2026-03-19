package com.hhp227.concafe.presentation.main.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.usecase.CreateVisitUseCase
import com.hhp227.concafe.domain.usecase.DismissReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ShouldShowReviewPromptUseCase

class CheckInViewModel(
    private val getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase,
    private val getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase,
    private val createVisitUseCase: CreateVisitUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val shouldShowReviewPromptUseCase: ShouldShowReviewPromptUseCase,
    private val dismissReviewPromptUseCase: DismissReviewPromptUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState.empty())

    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CheckInEvent>(replay = 0)

    val event = _event.asSharedFlow()

    private var observeSessionJob: Job? = null
    private var observeCafeDetailEventJob: Job? = null
    private var observeCastEventJob: Job? = null

    private fun loadGuestFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getCheckInGuestFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentLocationLabel = result.data.currentLocationLabel,
                            mapCafes = result.data.mapCafes,
                            popularCafes = result.data.popularCafes,
                            popularCasts = result.data.popularCasts
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun observeSession() {
        observeSessionJob = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoginPromptVisible = if (user == null) it.isLoginPromptVisible else false,
                        isNewVisitSheetVisible = if (user == null) it.isNewVisitSheetVisible else false,
                        reviewPrompt = if (user == null) null else it.reviewPrompt
                    )
                }

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            todayVisits = emptyList(),
                            recentVisits = emptyList()
                        )
                    }
                } else {
                    loadUserFeed()
                }
            }
        }
    }

    private fun loadUserFeed() {
        viewModelScope.launch {
            when (val result = getCheckInUserFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            todayVisits = result.data.todayVisits,
                            recentVisits = result.data.recentVisits
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            todayVisits = emptyList(),
                            recentVisits = emptyList(),
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun submitNewVisit(cafeId: String, visitedAt: String, memo: String?) {
        if (cafeId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "카페를 선택해 주세요.") }
            return
        }

        if (visitedAt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "방문 시간을 입력해 주세요.") }
            return
        }

        _uiState.update { it.copy(errorMessage = null) }

        viewModelScope.launch {
            when (val result = createVisitUseCase.invoke(
                cafeId = cafeId,
                visitedAt = visitedAt,
                memo = memo
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isNewVisitSheetVisible = false, errorMessage = null) }
                    loadUserFeed()
                    maybeShowReviewPrompt(result.data)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toString()) }
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        observeCafeDetailEventJob?.cancel()
        observeCafeDetailEventJob = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafe(event.cafe)
                }
            }
        }
    }

    private fun observeCastEvent() {
        observeCastEventJob?.cancel()
        observeCastEventJob = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> Unit
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                mapCafes = state.mapCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(name = cafe.name, locationLabel = cafe.region.city, rating = cafe.ratingAvg)
                    } else {
                        item
                    }
                },
                popularCafes = state.popularCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(name = cafe.name, locationLabel = cafe.region.city, rating = cafe.ratingAvg)
                    } else {
                        item
                    }
                },
                popularCasts = state.popularCasts.map { item ->
                    if (item.cafeId == cafe.id) item.copy(cafeName = cafe.name) else item
                },
                reviewPrompt = state.reviewPrompt?.let { prompt ->
                    if (prompt.cafeId == cafe.id) prompt.copy(cafeName = cafe.name) else prompt
                }
            )
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                popularCasts = state.popularCasts.map { item ->
                    if (item.id == cast.id) {
                        item.copy(name = cast.name, profileImage = cast.profileImage)
                    } else {
                        item
                    }
                }
            )
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(popularCasts = state.popularCasts.filterNot { it.id == castId })
        }
    }

    private suspend fun maybeShowReviewPrompt(visit: com.hhp227.concafe.domain.model.Visit) {
        if (!visit.verified) return

        val shouldShow = when (val result = shouldShowReviewPromptUseCase.invoke(visit.id)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> false
        }

        if (!shouldShow) return

        val cafeName = _uiState.value.mapCafes.firstOrNull { it.id == visit.cafeId }?.name
            ?: _uiState.value.popularCafes.firstOrNull { it.id == visit.cafeId }?.name
            ?: _uiState.value.todayVisits.firstOrNull { it.cafeId == visit.cafeId }?.cafeName
            ?: _uiState.value.recentVisits.firstOrNull { it.cafeId == visit.cafeId }?.cafeName
            ?: "방문한 카페"

        _uiState.update {
            it.copy(
                reviewPrompt = CheckInUiState.ReviewPrompt(
                    visitId = visit.id,
                    cafeId = visit.cafeId,
                    cafeName = cafeName
                )
            )
        }
    }

    private fun dismissReviewPrompt() {
        val prompt = _uiState.value.reviewPrompt ?: return
        viewModelScope.launch {
            dismissReviewPromptUseCase.invoke(prompt.visitId)
            _uiState.update { it.copy(reviewPrompt = null) }
        }
    }

    private fun clickWriteReviewPrompt() {
        val prompt = _uiState.value.reviewPrompt ?: return
        viewModelScope.launch {
            dismissReviewPromptUseCase.invoke(prompt.visitId)
            _uiState.update { it.copy(reviewPrompt = null) }
            _event.emit(CheckInEvent.NavigateToReviewEdit(prompt.cafeId))
        }
    }
    
    fun onAction(action: CheckInAction) {
        viewModelScope.launch {
            when (action) {
                is CheckInAction.ClickCafe -> _event.emit(CheckInEvent.NavigateToCafe(action.id))
                is CheckInAction.ClickCast -> _event.emit(CheckInEvent.NavigateToCast(action.id))
                CheckInAction.ClickCheckIn -> {
                    val currentUser = _uiState.value.currentUser

                    if (currentUser == null) {
                        _uiState.update {
                            it.copy(
                                isLoginPromptVisible = true,
                                isNewVisitSheetVisible = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isNewVisitSheetVisible = true) }
                    }
                }
                CheckInAction.ClickSignIn -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                    _event.emit(CheckInEvent.NavigateToSignIn)
                }
                CheckInAction.ClickSignUp -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                    _event.emit(CheckInEvent.NavigateToSignIn)
                }
                CheckInAction.DismissLoginPrompt -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                }
                CheckInAction.DismissNewVisitSheet -> {
                    _uiState.update { it.copy(isNewVisitSheetVisible = false) }
                }
                CheckInAction.DismissReviewPrompt -> dismissReviewPrompt()
                CheckInAction.ClickWriteReviewPrompt -> clickWriteReviewPrompt()
                is CheckInAction.SubmitNewVisit -> {
                    submitNewVisit(
                        cafeId = action.cafeId,
                        visitedAt = action.visitedAt,
                        memo = action.memo
                    )
                }
            }
        }
    }

    init {
        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        loadGuestFeed()
    }

    override fun onCleared() {
        observeSessionJob?.cancel()
        observeCafeDetailEventJob?.cancel()
        observeCastEventJob?.cancel()
        super.onCleared()
    }
}
