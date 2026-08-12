package com.hhp227.concafe.presentation.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.*
import com.hhp227.concafe.domain.event.publisher.*
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.usecase.*
import com.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty
import com.hhp227.concafe.presentation.theme.toPresentationContentLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeBannersUseCase: GetHomeBannersUseCase,
    private val getHomeCafeEventsUseCase: GetHomeCafeEventsUseCase,
    private val getNearbyCafePageUseCase: GetNearbyCafePageUseCase,
    private val getPopularCastPageUseCase: GetPopularCastPageUseCase,
    private val getBirthdayCastsUseCase: GetBirthdayCastsUseCase,
    private val getRecentNoticesUseCase: GetRecentNoticesUseCase,
    private val getCommunityPostPageUseCase: GetCommunityPostPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observeContentLayoutUseCase: ObserveContentLayoutUseCase,
    private val bannerEventPublisher: BannerEventPublisher,
    private val cafeEventEventPublisher: CafeEventEventPublisher,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher,
    private val communityPostEventPublisher: CommunityPostEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadInitialHomeSections() {
        jobs[TaskKey.INITIAL_HOME]?.cancel()
        jobs[TaskKey.INITIAL_HOME] = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val bannersDeferred = async { getHomeBannersUseCase.invoke(HOME_FEED_LIMIT) }
            val popularCastPageDeferred = async { getPopularCastPageUseCase.invoke(cursor = null) }
            val nearbyCafePageDeferred = async { getNearbyCafePageUseCase.invoke(cursor = null) }
            val birthdayCastsDeferred = async { getBirthdayCastsUseCase.invoke(HOME_FEED_LIMIT) }
            val noticesDeferred = async { getRecentNoticesUseCase.invoke(HOME_FEED_LIMIT) }
            val cafeEventsDeferred = async { getHomeCafeEventsUseCase.invoke(cursor = null, pageSize = MAX_HOME_CAFE_EVENTS) }
            val communityPostsDeferred = async {
                getCommunityPostPageUseCase.invoke(cursor = null, pageSize = MAX_HOME_COMMUNITY_POSTS)
            }
            val bannersResult = bannersDeferred.await()
            val popularCastPageResult = popularCastPageDeferred.await()
            val nearbyCafePageResult = nearbyCafePageDeferred.await()
            val birthdayCastsResult = birthdayCastsDeferred.await()
            val noticesResult = noticesDeferred.await()
            val cafeEventsResult = cafeEventsDeferred.await()
            val communityPostsResult = communityPostsDeferred.await()

            _uiState.update { state ->
                val popularCastPage = (popularCastPageResult as? AppResult.Success)?.data
                val nearbyCafePage = (nearbyCafePageResult as? AppResult.Success)?.data
                val cafeEventPage = (cafeEventsResult as? AppResult.Success)?.data
                val birthdayCastPage = (birthdayCastsResult as? AppResult.Success)?.data
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    banners = (bannersResult as? AppResult.Success)?.data ?: state.banners,
                    popularCasts = popularCastPage?.casts ?: state.popularCasts,
                    popularCastCafeNames = popularCastPage?.cafeNames ?: state.popularCastCafeNames,
                    popularCastCafeRegions = popularCastPage?.cafeRegions ?: state.popularCastCafeRegions,
                    popularCastCursor = popularCastPage?.nextCursor ?: state.popularCastCursor,
                    canLoadMorePopularCasts = popularCastPage?.hasNext ?: state.canLoadMorePopularCasts,
                    nearbyCafes = nearbyCafePage?.items ?: state.nearbyCafes,
                    nearbyCafeCursor = nearbyCafePage?.nextCursor ?: state.nearbyCafeCursor,
                    canLoadMoreNearbyCafes = nearbyCafePage?.hasNext ?: state.canLoadMoreNearbyCafes,
                    birthdayCasts = birthdayCastPage?.casts ?: state.birthdayCasts,
                    birthdayCastCafeNames = birthdayCastPage?.cafeNames ?: state.birthdayCastCafeNames,
                    notices = (noticesResult as? AppResult.Success)?.data ?: state.notices,
                    cafeEvents = cafeEventPage?.items ?: state.cafeEvents,
                    cafeEventCursor = cafeEventPage?.nextCursor ?: state.cafeEventCursor,
                    canLoadMoreCafeEvents = cafeEventPage?.hasNext ?: state.canLoadMoreCafeEvents,
                    isLoadingMoreCafeEvents = false,
                    communityPosts = (communityPostsResult as? AppResult.Success)?.data?.items ?: state.communityPosts
                )
            }
        }
    }

    private fun loadHomeBanners() {
        jobs[TaskKey.HOME_BANNERS]?.cancel()
        jobs[TaskKey.HOME_BANNERS] = viewModelScope.launch {
            when (val result = getHomeBannersUseCase.invoke(HOME_FEED_LIMIT)) {
                is AppResult.Success -> _uiState.update { it.copy(banners = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadBirthdayCasts() {
        jobs[TaskKey.BIRTHDAY_CASTS]?.cancel()
        jobs[TaskKey.BIRTHDAY_CASTS] = viewModelScope.launch {
            when (val result = getBirthdayCastsUseCase.invoke(HOME_FEED_LIMIT)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        birthdayCasts = result.data.casts,
                        birthdayCastCafeNames = result.data.cafeNames
                    )
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadRecentNotices() {
        jobs[TaskKey.RECENT_NOTICES]?.cancel()
        jobs[TaskKey.RECENT_NOTICES] = viewModelScope.launch {
            when (val result = getRecentNoticesUseCase.invoke(HOME_FEED_LIMIT)) {
                is AppResult.Success -> _uiState.update { it.copy(notices = result.data) }
                is AppResult.Failure -> Unit
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

    private fun loadHomeCafeEvents() {
        jobs[TaskKey.HOME_CAFE_EVENTS]?.cancel()
        jobs[TaskKey.HOME_CAFE_EVENTS] = viewModelScope.launch {
            when (val result = getHomeCafeEventsUseCase.invoke(cursor = null, pageSize = MAX_HOME_CAFE_EVENTS)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        cafeEvents = result.data.items
                            .filter { isDisplayableCafeEvent(it.statusLabel) }
                            .take(MAX_HOME_CAFE_EVENTS),
                        cafeEventCursor = result.data.nextCursor,
                        canLoadMoreCafeEvents = result.data.hasNext,
                        isLoadingMoreCafeEvents = false
                    )
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadHomeCafeEventPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.HOME_CAFE_EVENT_PAGE]?.cancel()
        jobs[TaskKey.HOME_CAFE_EVENT_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCafeEvents = append) }
            if (append) delay(PAGINATION_DELAY_MILLIS)
            when (val result = getHomeCafeEventsUseCase.invoke(cursor = cursor, pageSize = MAX_HOME_CAFE_EVENTS)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val events = result.data.items.filter { isDisplayableCafeEvent(it.statusLabel) }
                        state.copy(
                            cafeEvents = if (append) state.cafeEvents + events else events,
                            cafeEventCursor = result.data.nextCursor,
                            canLoadMoreCafeEvents = result.data.hasNext,
                            isLoadingMoreCafeEvents = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreCafeEvents = false, canLoadMoreCafeEvents = false) }
                }
            }
        }
    }

    private fun loadMoreCafeEvents() {
        val currentState = _uiState.value
        val cursor = currentState.cafeEventCursor
        if (currentState.isLoadingMoreCafeEvents || !currentState.canLoadMoreCafeEvents || cursor == null) return
        loadHomeCafeEventPage(cursor = cursor, append = true)
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
                            popularCastCafeRegions = if (append) {
                                state.popularCastCafeRegions + result.data.cafeRegions
                            } else {
                                result.data.cafeRegions
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
                    is BannerEvent.Created -> loadHomeBanners()
                    is BannerEvent.Updated -> loadHomeBanners()
                    is BannerEvent.Deleted -> loadHomeBanners()
                }
            }
        }
    }

    private fun observeContentLayout() {
        jobs[TaskKey.OBSERVE_CONTENT_LAYOUT]?.cancel()
        jobs[TaskKey.OBSERVE_CONTENT_LAYOUT] = viewModelScope.launch {
            observeContentLayoutUseCase.invoke().collect { contentLayout ->
                _uiState.update { it.copy(contentLayout = contentLayout.toPresentationContentLayout()) }
            }
        }
    }

    private fun observeCafeRegistrationClaimEvent() {
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT] = viewModelScope.launch {
            cafeRegistrationClaimEventPublisher.events.collectLatest { event ->
                if (event is CafeRegistrationClaimEvent.Approved) {
                    loadNearbyCafePage(cursor = null, append = false)
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> {
                        loadPopularCastPage(cursor = null, append = false)
                        loadBirthdayCasts()
                    }
                    is CastEvent.Updated -> {
                        loadPopularCastPage(cursor = null, append = false)
                        loadBirthdayCasts()
                    }
                    is CastEvent.Deleted -> {
                        loadPopularCastPage(cursor = null, append = false)
                        loadBirthdayCasts()
                    }
                }
            }
        }
    }

    private fun observeCommunityPostEvents() {
        jobs[TaskKey.COMMUNITY_POST_EVENT]?.cancel()
        jobs[TaskKey.COMMUNITY_POST_EVENT] = viewModelScope.launch {
            communityPostEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CommunityPostEvent.Created -> loadCommunityPosts()
                    is CommunityPostEvent.Deleted -> _uiState.update { state ->
                        state.copy(communityPosts = state.communityPosts.filterNot { it.id == event.postId })
                    }
                    is CommunityPostEvent.Updated -> _uiState.update { state ->
                        state.copy(communityPosts = state.communityPosts.map { if (it.id == event.post.id) event.post else it })
                    }
                }
            }
        }
    }

    private fun observeCafeEventEvent() {
        jobs[TaskKey.OBSERVE_CAFE_EVENT_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_EVENT_EVENT] = viewModelScope.launch {
            cafeEventEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CafeEventEvent.Created -> loadHomeCafeEvents()
                    is CafeEventEvent.Updated -> loadHomeCafeEvents()
                    is CafeEventEvent.Deleted -> loadHomeCafeEvents()
                }
            }
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                popularCastCafeNames = state.popularCastCafeNames + (cafe.id to cafe.name),
                popularCastCafeRegions = state.popularCastCafeRegions + (cafe.id to cafe.region.city),
                birthdayCastCafeNames = state.birthdayCastCafeNames + (cafe.id to cafe.name),
                nearbyCafes = state.nearbyCafes.map { item ->
                    if (item.id == cafe.id) cafe else item
                }
            )
        }
    }


    private fun isDisplayableCafeEvent(statusLabel: String): Boolean {
        val normalized = statusLabel.trim().lowercase()
        return normalized.contains("진행 중") ||
            normalized.contains("진행중") ||
            normalized.contains("ongoing") ||
            normalized.contains("예정") ||
            normalized.contains("upcoming") ||
            normalized.contains("scheduled")
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
                HomeAction.LoadMoreCafeEvents -> loadMoreCafeEvents()
                HomeAction.LoadMorePopularCasts -> loadMorePopularCasts()
                HomeAction.LoadMoreNearbyCafes -> loadMoreNearbyCafes()
                HomeAction.ClickCommunity -> requireSignedIn {
                    _event.emit(HomeEvent.NavigateToCommunity)
                }
                is HomeAction.ClickCommunityPost -> requireSignedIn {
                    _event.emit(HomeEvent.NavigateToPostDetail(action.postId))
                }
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
        observeContentLayout()
        observeBannerEvent()
        observeCafeEventEvent()
        observeCafeRegistrationClaimEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        observeCommunityPostEvents()
        loadInitialHomeSections()
    }

    private enum class TaskKey {
        INITIAL_HOME,
        HOME_BANNERS,
        BIRTHDAY_CASTS,
        RECENT_NOTICES,
        HOME_CAFE_EVENTS,
        HOME_CAFE_EVENT_PAGE,
        OBSERVE_BANNER_EVENT,
        OBSERVE_CONTENT_LAYOUT,
        OBSERVE_CAFE_EVENT_EVENT,
        OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_SESSION,
        COMMUNITY_POST_EVENT,
        POPULAR_CAST_PAGE,
        NEARBY_CAFE_PAGE,
        COMMUNITY_POSTS
    }

    private companion object {
        private const val HOME_FEED_LIMIT = 6
        private const val MAX_HOME_CAFE_EVENTS = 8
        private const val MAX_HOME_COMMUNITY_POSTS = 8
        private const val PAGINATION_DELAY_MILLIS = 1_000L
    }
}
