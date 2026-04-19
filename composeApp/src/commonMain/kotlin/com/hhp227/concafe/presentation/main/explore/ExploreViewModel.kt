package com.hhp227.concafe.presentation.main.explore

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
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.usecase.GetExploreCafePageUseCase
import com.hhp227.concafe.domain.usecase.GetExploreCastPageUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.presentation.main.explore.ExploreUiState.Companion.empty

class ExploreViewModel(
    private val getExploreCafePageUseCase: GetExploreCafePageUseCase,
    private val getExploreCastPageUseCase: GetExploreCastPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ExploreEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isLoginPromptVisible = if (user == null) it.isLoginPromptVisible else false
                    )
                }
            }
        }
    }

    private fun observeCafeRegistrationClaimEvent() {
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT] = viewModelScope.launch {
            cafeRegistrationClaimEventPublisher.events.collectLatest { event ->
                if (event is CafeRegistrationClaimEvent.Approved) {
                    val approvedCafeId = event.approvedCafeId?.trim().orEmpty()
                    val alreadyVisible = approvedCafeId.isNotEmpty() &&
                        _uiState.value.cafes.any { cafe -> cafe.id == approvedCafeId }

                    if (!alreadyVisible) {
                        loadCafePage(cursor = null, append = false)
                    }
                }
            }
        }
    }

    private fun refreshCurrentTab() {
        jobs[TaskKey.CAFE_PAGE]?.cancel()
        jobs[TaskKey.MAID_PAGE]?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                cafes = if (it.selectedTab == ExploreUiState.TabType.CAFE) emptyList() else it.cafes,
                cafesNextCursor = if (it.selectedTab == ExploreUiState.TabType.CAFE) null else it.cafesNextCursor,
                canLoadMoreCafes = if (it.selectedTab == ExploreUiState.TabType.CAFE) false else it.canLoadMoreCafes,
                isLoadingMoreCafes = false,
                maids = if (it.selectedTab == ExploreUiState.TabType.MAID) emptyList() else it.maids,
                maidsNextCursor = if (it.selectedTab == ExploreUiState.TabType.MAID) null else it.maidsNextCursor,
                canLoadMoreMaids = if (it.selectedTab == ExploreUiState.TabType.MAID) false else it.canLoadMoreMaids,
                isLoadingMoreMaids = false
            )
        }
        if (_uiState.value.selectedTab == ExploreUiState.TabType.CAFE) {
            loadCafePage(cursor = null, append = false)
        } else {
            loadMaidPage(cursor = null, append = false)
        }
    }

    private fun loadCafePage(cursor: String?, append: Boolean) {
        jobs[TaskKey.CAFE_PAGE]?.cancel()
        jobs[TaskKey.CAFE_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCafes = append) }
            val state = _uiState.value
            when (
                val result = getExploreCafePageUseCase.invoke(
                    query = state.query,
                    regionKey = state.selectedRegion.key,
                    sortKey = state.selectedSort.key,
                    cursor = cursor,
                    pageSize = PAGE_SIZE
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            cafes = if (append) it.cafes + result.data.items else result.data.items,
                            cafesNextCursor = result.data.nextCursor,
                            canLoadMoreCafes = result.data.hasNext,
                            isLoadingMoreCafes = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString(),
                            isLoadingMoreCafes = false
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreCafes() {
        val state = _uiState.value
        val cursor = state.cafesNextCursor
        if (state.isLoadingMoreCafes || !state.canLoadMoreCafes || cursor == null) return
        loadCafePage(cursor, append = true)
    }

    private fun loadMaidPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.MAID_PAGE]?.cancel()
        jobs[TaskKey.MAID_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMaids = append) }
            val state = _uiState.value
            when (
                val result = getExploreCastPageUseCase.invoke(
                    query = state.query,
                    regionKey = state.selectedRegion.key,
                    sortKey = state.selectedSort.key,
                    cursor = cursor,
                    pageSize = PAGE_SIZE
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            maids = if (append) it.maids + result.data.items else result.data.items,
                            maidsNextCursor = result.data.nextCursor,
                            canLoadMoreMaids = result.data.hasNext,
                            isLoadingMoreMaids = false
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.error.toString(),
                        isLoadingMoreMaids = false
                    )
                }
            }
        }
    }

    private fun loadMoreMaids() {
        val state = _uiState.value
        val cursor = state.maidsNextCursor
        if (state.isLoadingMoreMaids || !state.canLoadMoreMaids || cursor == null) return
        loadMaidPage(cursor, append = true)
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
                    is CastEvent.Created -> addCastIfVisible(event.cast)
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            val cafeMatches = matchesCafeFilters(state, cafe)
            val nextCafes = state.cafes.mapNotNull { item ->
                when {
                    item.id != cafe.id -> item
                    cafeMatches -> cafe
                    else -> null
                }
            }.sortedCafes(state)
            val nextMaids = state.maids.filter { maid ->
                if (maid.cafeId != cafe.id) {
                    true
                } else {
                    matchesCastFilters(state, maid, cafeMatches)
                }
            }.sortedCasts(state)
            state.copy(cafes = nextCafes, maids = nextMaids)
        }
    }

    private fun addCastIfVisible(cast: Cast) {
        _uiState.update { state ->
            if (state.maids.any { it.id == cast.id } || !matchesCastFilters(state, cast)) {
                state
            } else {
                state.copy(maids = (state.maids + cast).sortedCasts(state))
            }
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            val nextMaids = state.maids.mapNotNull { item ->
                when {
                    item.id != cast.id -> item
                    matchesCastFilters(state, cast) -> cast
                    else -> null
                }
            }.sortedCasts(state)
            state.copy(maids = nextMaids)
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(maids = state.maids.filterNot { it.id == castId })
        }
    }

    fun onAction(action: ExploreAction) {
        when (action) {
            is ExploreAction.QueryChanged -> {
                _uiState.update { it.copy(query = action.query) }
                refreshCurrentTab()
            }
            is ExploreAction.RegionChanged -> {
                _uiState.update { it.copy(selectedRegion = action.region) }
                refreshCurrentTab()
            }
            is ExploreAction.SortChanged -> {
                _uiState.update { it.copy(selectedSort = action.sort) }
                refreshCurrentTab()
            }
            is ExploreAction.TabChanged -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
                val nextState = _uiState.value
                if (action.tab == ExploreUiState.TabType.CAFE && nextState.cafes.isEmpty()) {
                    refreshCurrentTab()
                } else if (action.tab == ExploreUiState.TabType.MAID && nextState.maids.isEmpty()) {
                    refreshCurrentTab()
                }
            }
            is ExploreAction.ClickCafe -> {
                viewModelScope.launch { requireSignedIn { _event.emit(ExploreEvent.NavigateToCafe(action.id)) } }
            }
            is ExploreAction.ClickMaid -> {
                viewModelScope.launch { requireSignedIn { _event.emit(ExploreEvent.NavigateToCast(action.id)) } }
            }
            ExploreAction.ClickLoginPromptSignIn -> viewModelScope.launch {
                _uiState.update { it.copy(isLoginPromptVisible = false) }
                _event.emit(ExploreEvent.NavigateToSignIn)
            }
            ExploreAction.DismissLoginPrompt -> _uiState.update { it.copy(isLoginPromptVisible = false) }
            ExploreAction.LoadMoreCafes -> loadMoreCafes()
            ExploreAction.LoadMoreMaids -> loadMoreMaids()
            is ExploreAction.Refresh -> refreshCurrentTab()
        }
    }

    private suspend fun requireSignedIn(onAuthenticated: suspend () -> Unit) {
        if (_uiState.value.isLoggedIn) {
            onAuthenticated()
        } else {
            _uiState.update { it.copy(isLoginPromptVisible = true) }
        }
    }

    init {
        observeSession()
        observeCafeRegistrationClaimEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        refreshCurrentTab()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_SESSION,
        CAFE_PAGE,
        MAID_PAGE
    }

    private companion object {
        private const val PAGE_SIZE = 15
    }
}

private fun matchesCafeFilters(state: ExploreUiState, cafe: Cafe): Boolean {
    val query = state.query.trim()
    val matchesQuery = query.isBlank() || cafe.name.contains(query, ignoreCase = true)
    val matchesRegion = when (state.selectedRegion) {
        ExploreUiState.RegionFilter.ALL -> true
        ExploreUiState.RegionFilter.SEOUL -> cafe.region.country.equals("KR", ignoreCase = true) &&
            cafe.region.city.equals("Seoul", ignoreCase = true)
        ExploreUiState.RegionFilter.BUSAN -> cafe.region.country.equals("KR", ignoreCase = true) &&
                cafe.region.city.equals("Busan", ignoreCase = true)
        ExploreUiState.RegionFilter.DAEGU -> cafe.region.country.equals("KR", ignoreCase = true) &&
                cafe.region.city.equals("Daegu", ignoreCase = true)
        ExploreUiState.RegionFilter.TOKYO -> cafe.region.country.equals("JP", ignoreCase = true) &&
            cafe.region.city.equals("Tokyo", ignoreCase = true)
        ExploreUiState.RegionFilter.OSAKA -> cafe.region.country.equals("JP", ignoreCase = true) &&
            cafe.region.city.equals("Osaka", ignoreCase = true)
        ExploreUiState.RegionFilter.YOKOHAMA -> cafe.region.country.equals("JP", ignoreCase = true) &&
            cafe.region.city.equals("Yokohama", ignoreCase = true)
    }
    return matchesQuery && matchesRegion
}

private fun matchesCastFilters(state: ExploreUiState, cast: Cast, cafeVisible: Boolean? = null): Boolean {
    val query = state.query.trim()
    val matchesQuery = query.isBlank() || cast.name.contains(query, ignoreCase = true)
    val matchesRegion = cafeVisible ?: when (state.selectedRegion) {
        ExploreUiState.RegionFilter.ALL -> true
        else -> state.cafes.any { it.id == cast.cafeId }
    }
    return matchesQuery && matchesRegion
}

private fun List<Cafe>.sortedCafes(state: ExploreUiState): List<Cafe> {
    return when (state.selectedSort) {
        ExploreUiState.SortFilter.POPULAR -> sortedByDescending { it.reviewCount }
        ExploreUiState.SortFilter.LATEST -> sortedByDescending { it.id }
        ExploreUiState.SortFilter.RATING -> sortedByDescending { it.ratingAvg }
    }
}

private fun List<Cast>.sortedCasts(state: ExploreUiState): List<Cast> {
    return when (state.selectedSort) {
        ExploreUiState.SortFilter.POPULAR,
        ExploreUiState.SortFilter.RATING -> sortedByDescending { it.followerCount }
        ExploreUiState.SortFilter.LATEST -> sortedByDescending { it.id }
    }
}
