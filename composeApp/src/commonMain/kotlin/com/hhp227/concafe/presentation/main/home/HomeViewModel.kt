package com.hhp227.concafe.presentation.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.event.CafeEventEvent
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeEventEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeCafeEvent
import com.hhp227.concafe.domain.usecase.GetCommunityPostPageUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.GetNearbyCafePageUseCase
import com.hhp227.concafe.domain.usecase.GetPopularCastPageUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val getNearbyCafePageUseCase: GetNearbyCafePageUseCase,
    private val getPopularCastPageUseCase: GetPopularCastPageUseCase,
    private val getCommunityPostPageUseCase: GetCommunityPostPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val bannerEventPublisher: BannerEventPublisher,
    private val cafeEventEventPublisher: CafeEventEventPublisher,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = getHomeFeedUseCase.invoke()

            if (result is AppResult.Success) {
                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        errorMessage = null,
                        banners = result.data.banners,
                        popularCasts = result.data.popularCasts,
                        popularCastCafeNames = result.data.popularCastCafeNames,
                        popularCastCursor = result.data.popularCastsNextCursor,
                        canLoadMorePopularCasts = result.data.hasMorePopularCasts,
                        nearbyCafes = result.data.nearbyCafes,
                        nearbyCafeCursor = result.data.nearbyCafesNextCursor,
                        canLoadMoreNearbyCafes = result.data.hasMoreNearbyCafes,
                        birthdayCasts = result.data.birthdayCasts,
                        notices = result.data.notices,
                        cafeEvents = result.data.cafeEvents
                            .filter { isOngoingCafeEvent(it.statusLabel) }
                            .take(MAX_HOME_CAFE_EVENTS)
                    )
                }
            } else if (result is AppResult.Failure) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = result.error.toString()
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "unknown"
                    )
                }
            }
        }
    }

    private fun loadCommunityPosts() {
        jobs[TaskKey.COMMUNITY_POSTS]?.cancel()
        jobs[TaskKey.COMMUNITY_POSTS] = viewModelScope.launch {
            when (val result = getCommunityPostPageUseCase.invoke(cursor = null, pageSize = MAX_HOME_COMMUNITY_POSTS)) {
                is AppResult.Success -> _uiState.update { it.copy(communityPosts = result.data.items) }
                else -> Unit
            }
        }
    }

    private fun loadPopularCastPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.POPULAR_CAST_PAGE]?.cancel()
        jobs[TaskKey.POPULAR_CAST_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMorePopularCasts = append) }
            if (append) delay(PAGINATION_DELAY_MILLIS)
            when (val result = getPopularCastPageUseCase.invoke(cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            popularCasts = if (append) state.popularCasts + result.data.casts else result.data.casts,
                            popularCastCafeNames = if (append) {
                                state.popularCastCafeNames + result.data.cafeNames
                            } else {
                                result.data.cafeNames
                            },
                            popularCastCursor = result.data.nextCursor,
                            canLoadMorePopularCasts = result.data.hasNext,
                            isLoadingMorePopularCasts = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMorePopularCasts = false) }
                }
            }
        }
    }

    private fun loadMorePopularCasts() {
        val currentState = _uiState.value
        val cursor = currentState.popularCastCursor
        if (currentState.isLoadingMorePopularCasts || !currentState.canLoadMorePopularCasts || cursor == null) return
        loadPopularCastPage(cursor = cursor, append = true)
    }

    private fun loadNearbyCafePage(cursor: String?, append: Boolean) {
        jobs[TaskKey.NEARBY_CAFE_PAGE]?.cancel()
        jobs[TaskKey.NEARBY_CAFE_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreNearbyCafes = append) }
            if (append) delay(PAGINATION_DELAY_MILLIS)
            when (val result = getNearbyCafePageUseCase.invoke(cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            nearbyCafes = if (append) state.nearbyCafes + result.data.items else result.data.items,
                            nearbyCafeCursor = result.data.nextCursor,
                            canLoadMoreNearbyCafes = result.data.hasNext,
                            isLoadingMoreNearbyCafes = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreNearbyCafes = false, canLoadMoreNearbyCafes = false) }
                }
            }
        }
    }

    private fun loadMoreNearbyCafes() {
        val currentState = _uiState.value
        val cursor = currentState.nearbyCafeCursor
        if (currentState.isLoadingMoreNearbyCafes || !currentState.canLoadMoreNearbyCafes || cursor == null) return
        loadNearbyCafePage(cursor = cursor, append = true)
    }

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

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeInfo(event.cafe)
                }
            }
        }
    }

    private fun observeBannerEvent() {
        jobs[TaskKey.OBSERVE_BANNER_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_BANNER_EVENT] = viewModelScope.launch {
            bannerEventPublisher.events.collectLatest { event ->
                when (event) {
                    is BannerEvent.Created -> loadHomeFeed()
                    is BannerEvent.Updated -> patchBanner(event.banner)
                    is BannerEvent.Deleted -> removeBanner(event.banner.id)
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
                        _uiState.value.nearbyCafes.any { cafe -> cafe.id == approvedCafeId }

                    if (!alreadyVisible) {
                        loadNearbyCafePage(cursor = null, append = false)
                    }
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> loadPopularCastPage(cursor = null, append = false)
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun observeCafeEventEvent() {
        jobs[TaskKey.OBSERVE_CAFE_EVENT_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_EVENT_EVENT] = viewModelScope.launch {
            cafeEventEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CafeEventEvent.Created -> upsertCafeEvent(event.event)
                    is CafeEventEvent.Updated -> upsertCafeEvent(event.event)
                    is CafeEventEvent.Deleted -> removeCafeEvent(event.eventId)
                }
            }
        }
    }

    private fun patchBanner(updatedBanner: HomeBanner) {
        _uiState.update { state ->
            state.copy(
                banners = state.banners.map { banner ->
                    if (banner.id == updatedBanner.id) updatedBanner else banner
                }
            )
        }
    }

    private fun removeBanner(bannerId: String) {
        _uiState.update { state ->
            state.copy(
                banners = state.banners.filterNot { it.id == bannerId }
            )
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                popularCastCafeNames = state.popularCastCafeNames + (cafe.id to cafe.name),
                nearbyCafes = state.nearbyCafes.map { item ->
                    if (item.id == cafe.id) cafe else item
                }
            )
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                popularCasts = state.popularCasts.map { item ->
                    if (item.id == cast.id) cast else item
                },
                birthdayCasts = state.birthdayCasts.map { item ->
                    if (item.id == cast.id) cast else item
                }
            )
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(
                popularCasts = state.popularCasts.filterNot { it.id == castId },
                birthdayCasts = state.birthdayCasts.filterNot { it.id == castId }
            )
        }
    }

    private fun upsertCafeEvent(event: CafeEventManagementItem) {
        val homeEvent = toHomeCafeEvent(event, _uiState.value) ?: run {
            removeCafeEvent(event.id)
            return
        }
        _uiState.update { state ->
            val merged = (state.cafeEvents.filterNot { it.id == homeEvent.id } + homeEvent)
                .sortedByDescending { it.periodText }
                .take(MAX_HOME_CAFE_EVENTS)
            state.copy(cafeEvents = merged)
        }
    }

    private fun removeCafeEvent(eventId: String) {
        _uiState.update { state ->
            state.copy(cafeEvents = state.cafeEvents.filterNot { it.id == eventId })
        }
    }

    private fun toHomeCafeEvent(event: CafeEventManagementItem, currentState: HomeUiState): HomeCafeEvent? {
        if (!isOngoingCafeEvent(event.statusLabel) || event.isDimmed) {
            return null
        }
        val cafeName = currentState.nearbyCafes.firstOrNull { it.id == event.cafeId }?.name
            ?: currentState.popularCastCafeNames[event.cafeId]
            ?: event.cafeId
        return HomeCafeEvent(
            id = event.id,
            cafeId = event.cafeId,
            cafeName = cafeName,
            title = event.title,
            content = event.content,
            imageUrl = event.imageUrl,
            periodText = event.periodText,
            statusLabel = event.statusLabel
        )
    }

    private fun isOngoingCafeEvent(statusLabel: String): Boolean {
        val normalized = statusLabel.trim().lowercase()
        return normalized.contains("진행 중") || normalized.contains("진행중") || normalized.contains("ongoing")
    }

    private suspend fun requireSignedIn(onAuthenticated: suspend () -> Unit) {
        if (_uiState.value.isLoggedIn) {
            onAuthenticated()
        } else {
            _uiState.update { it.copy(isLoginPromptVisible = true) }
        }
    }

    private suspend fun handleBannerClick(banner: HomeBanner) {
        when (banner.targetType) {
            BannerLinkTargetType.EXTERNAL_LINK -> {
                if (banner.targetValue.isNotBlank()) {
                    _event.emit(
                        HomeEvent.NavigateToExternalLink(
                            title = banner.title,
                            url = banner.targetValue
                        )
                    )
                }
            }
            BannerLinkTargetType.EVENT_DETAIL -> {
                val cafeId = banner.cafeId
                val eventId = banner.targetValue.trim()
                if (!cafeId.isNullOrBlank() && eventId.isNotBlank()) {
                    _event.emit(HomeEvent.NavigateToCafeEvent(cafeId, eventId))
                }
            }
            BannerLinkTargetType.CAFE_DETAIL,
            BannerLinkTargetType.NOTICE -> {
                val cafeId = banner.cafeId ?: banner.targetValue.takeIf { banner.targetType == BannerLinkTargetType.CAFE_DETAIL }
                if (!cafeId.isNullOrBlank()) {
                    requireSignedIn {
                        _event.emit(HomeEvent.NavigateToCafe(cafeId))
                    }
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            when (action) {
                is HomeAction.ClickBanner -> handleBannerClick(action.banner)
                is HomeAction.ClickMaid -> requireSignedIn {
                    _event.emit(HomeEvent.NavigateToCast(action.id))
                }
                is HomeAction.ClickBirthdayMaid -> requireSignedIn {
                    _event.emit(HomeEvent.NavigateToCast(action.id))
                }
                is HomeAction.ClickCafe -> requireSignedIn {
                    _event.emit(HomeEvent.NavigateToCafe(action.id))
                }
                is HomeAction.ClickCafeEvent -> {
                    _event.emit(HomeEvent.NavigateToCafeEvent(action.cafeId, action.eventId))
                }
                HomeAction.ClickLoginPromptSignIn -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                    _event.emit(HomeEvent.NavigateToSignIn)
                }
                HomeAction.DismissLoginPrompt -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                }
                HomeAction.LoadMorePopularCasts -> loadMorePopularCasts()
                HomeAction.LoadMoreNearbyCafes -> loadMoreNearbyCafes()
                HomeAction.ClickCommunity -> _event.emit(HomeEvent.NavigateToCommunity)
                is HomeAction.ClickCommunityPost -> _event.emit(HomeEvent.NavigateToPostDetail(action.postId))
            }
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeSession()
        observeBannerEvent()
        observeCafeEventEvent()
        observeCafeRegistrationClaimEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        loadHomeFeed()
        loadCommunityPosts()
    }

    private enum class TaskKey {
        OBSERVE_BANNER_EVENT,
        OBSERVE_CAFE_EVENT_EVENT,
        OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_SESSION,
        POPULAR_CAST_PAGE,
        NEARBY_CAFE_PAGE,
        COMMUNITY_POSTS
    }

    private companion object {
        private const val MAX_HOME_CAFE_EVENTS = 8
        private const val MAX_HOME_COMMUNITY_POSTS = 8
        private const val PAGINATION_DELAY_MILLIS = 1_000L
    }
}
