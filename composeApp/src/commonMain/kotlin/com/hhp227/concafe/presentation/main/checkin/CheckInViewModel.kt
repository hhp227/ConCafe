package com.hhp227.concafe.presentation.main.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.core.util.TimeUtils
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
import com.hhp227.concafe.domain.event.VisitEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.VisitEventPublisher
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
    private val castEventPublisher: CastEventPublisher,
    private val visitEventPublisher: VisitEventPublisher,
    private val checkInLocationProvider: CheckInLocationProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CheckInEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadGuestFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        jobs[TaskKey.LOAD_GUEST_FEED]?.cancel()
        jobs[TaskKey.LOAD_GUEST_FEED] = viewModelScope.launch {
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
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
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
                            recentVisits = emptyList(),
                            recentVisitsNextCursor = null,
                            canLoadMoreRecentVisits = false,
                            isLoadingMoreRecentVisits = false
                        )
                    }
                } else {
                    refreshRecentVisitPage()
                }
            }
        }
    }

    private fun loadRecentVisitPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.LOAD_USER_VISIT_PAGE]?.cancel()
        jobs[TaskKey.LOAD_USER_VISIT_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreRecentVisits = append) }

            when (val result = getCheckInUserFeedUseCase.invoke(cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val mergedRecentVisits = if (append) {
                            state.recentVisits + result.data.recentVisits
                        } else {
                            result.data.recentVisits
                        }
                        val mergedTodayVisits = mergedRecentVisits
                            .filter { visit -> TimeUtils.isCurrentDateVisitedAt(visit.visitedAt) }
                            .take(TODAY_VISIT_LIMIT)

                        state.copy(
                            todayVisits = mergedTodayVisits,
                            recentVisits = mergedRecentVisits,
                            recentVisitsNextCursor = result.data.recentVisitsNextCursor,
                            canLoadMoreRecentVisits = result.data.canLoadMoreRecentVisits,
                            isLoadingMoreRecentVisits = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        if (append) {
                            state.copy(
                                isLoadingMoreRecentVisits = false,
                                errorMessage = result.error.toString()
                            )
                        } else {
                            state.copy(
                                todayVisits = emptyList(),
                                recentVisits = emptyList(),
                                recentVisitsNextCursor = null,
                                canLoadMoreRecentVisits = false,
                                isLoadingMoreRecentVisits = false,
                                errorMessage = result.error.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun refreshRecentVisitPage() {
        loadRecentVisitPage(cursor = null, append = false)
    }

    private fun loadMoreRecentVisitPage() {
        val currentState = _uiState.value
        val cursor = currentState.recentVisitsNextCursor

        if (
            currentState.isLoadingMoreRecentVisits ||
            !currentState.canLoadMoreRecentVisits ||
            cursor == null
        ) {
            Unit
        } else {
            loadRecentVisitPage(cursor = cursor, append = true)
        }
    }

    private fun submitNewVisit(cafeId: String, visitedAt: String, memo: String?) {
        if (cafeId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "카페를 선택해 주세요.") }
        } else if (visitedAt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "방문 시간을 입력해 주세요.") }
        } else {
            _uiState.update { it.copy(errorMessage = null) }

            jobs[TaskKey.SUBMIT_VISIT]?.cancel()
            jobs[TaskKey.SUBMIT_VISIT] = viewModelScope.launch {
                when (val locationResult = checkInLocationProvider.getCurrentLocation()) {
                    is CheckInLocationResult.Failure -> {
                        _uiState.update {
                            it.copy(errorMessage = locationResult.message)
                        }
                    }
                    is CheckInLocationResult.Success -> {
                        when (val result = createVisitUseCase.invoke(
                            cafeId = cafeId,
                            visitedAt = visitedAt,
                            memo = memo,
                            latitude = locationResult.location.latitude,
                            longitude = locationResult.location.longitude
                        )) {
                            is AppResult.Success -> {
                                _uiState.update { it.copy(isNewVisitSheetVisible = false, errorMessage = null) }
                                refreshRecentVisitPage()
                                maybeShowReviewPrompt(result.data)
                            }
                            is AppResult.Failure -> {
                                _uiState.update { it.copy(errorMessage = result.error.toString()) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clickCheckIn() {
        val currentUser = _uiState.value.currentUser

        if (currentUser == null) {
            _uiState.update {
                it.copy(
                    isLoginPromptVisible = true,
                    isNewVisitSheetVisible = false
                )
            }
        } else {
            jobs[TaskKey.REQUEST_LOCATION_PERMISSION]?.cancel()
            jobs[TaskKey.REQUEST_LOCATION_PERMISSION] = viewModelScope.launch {
                when (val permissionResult = checkInLocationProvider.requestPermissionIfNeeded()) {
                    CheckInLocationPermissionResult.Granted -> {
                        _uiState.update {
                            it.copy(
                                isNewVisitSheetVisible = true,
                                errorMessage = null
                            )
                        }
                    }
                    is CheckInLocationPermissionResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isNewVisitSheetVisible = false,
                                errorMessage = permissionResult.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafe(event.cafe)
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> Unit
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun observeVisitEvent() {
        jobs[TaskKey.OBSERVE_VISIT_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_VISIT_EVENT] = viewModelScope.launch {
            visitEventPublisher.events.collectLatest { event ->
                when (event) {
                    is VisitEvent.Created -> refreshRecentVisitPage()
                    is VisitEvent.Deleted -> refreshRecentVisitPage()
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
                recentVisits = state.recentVisits.map { item ->
                    if (item.cafeId == cafe.id) item.copy(cafeName = cafe.name, cafeImage = cafe.thumbnailImage.orEmpty()) else item
                },
                todayVisits = state.todayVisits.map { item ->
                    if (item.cafeId == cafe.id) item.copy(cafeName = cafe.name, cafeImage = cafe.thumbnailImage.orEmpty()) else item
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
        jobs[TaskKey.REVIEW_PROMPT_ACTION]?.cancel()
        jobs[TaskKey.REVIEW_PROMPT_ACTION] = viewModelScope.launch {
            dismissReviewPromptUseCase.invoke(prompt.visitId)
            _uiState.update { it.copy(reviewPrompt = null) }
        }
    }

    private fun clickWriteReviewPrompt() {
        val prompt = _uiState.value.reviewPrompt ?: return
        jobs[TaskKey.REVIEW_PROMPT_ACTION]?.cancel()
        jobs[TaskKey.REVIEW_PROMPT_ACTION] = viewModelScope.launch {
            dismissReviewPromptUseCase.invoke(prompt.visitId)
            _uiState.update { it.copy(reviewPrompt = null) }
            _event.emit(CheckInEvent.NavigateToReviewEdit(prompt.cafeId))
        }
    }
    
    fun onAction(action: CheckInAction) {
        viewModelScope.launch {
            when (action) {
                is CheckInAction.ClickCafe -> {
                    if (_uiState.value.currentUser == null) {
                        _uiState.update {
                            it.copy(
                                isLoginPromptVisible = true,
                                isNewVisitSheetVisible = false
                            )
                        }
                    } else {
                        _event.emit(CheckInEvent.NavigateToCafe(action.id))
                    }
                }
                is CheckInAction.ClickCast -> {
                    if (_uiState.value.currentUser == null) {
                        _uiState.update {
                            it.copy(
                                isLoginPromptVisible = true,
                                isNewVisitSheetVisible = false
                            )
                        }
                    } else {
                        _event.emit(CheckInEvent.NavigateToCast(action.id))
                    }
                }
                CheckInAction.ClickCheckIn -> {
                    clickCheckIn()
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
                CheckInAction.DismissError -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
                CheckInAction.DismissNewVisitSheet -> {
                    _uiState.update { it.copy(isNewVisitSheetVisible = false) }
                }
                CheckInAction.DismissReviewPrompt -> dismissReviewPrompt()
                CheckInAction.ClickWriteReviewPrompt -> clickWriteReviewPrompt()
                CheckInAction.LoadMoreRecentVisits -> loadMoreRecentVisitPage()
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
        observeVisitEvent()
        loadGuestFeed()
    }

    override fun onCleared() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        LOAD_GUEST_FEED,
        OBSERVE_SESSION,
        LOAD_USER_VISIT_PAGE,
        SUBMIT_VISIT,
        REQUEST_LOCATION_PERMISSION,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_VISIT_EVENT,
        REVIEW_PROMPT_ACTION
    }

    private companion object {
        private const val TODAY_VISIT_LIMIT = 4
    }
}
