package com.hhp227.concafe.presentation.main.myinfo

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
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.UserEvent
import com.hhp227.concafe.domain.event.VisitEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.UserEventPublisher
import com.hhp227.concafe.domain.event.publisher.VisitEventPublisher
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.presentation.main.myinfo.MyInfoEvent.*
import com.hhp227.concafe.presentation.main.myinfo.MyInfoUiState.Companion.empty
import kotlin.math.max

class MyInfoViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher,
    private val visitEventPublisher: VisitEventPublisher,
    private val userEventPublisher: UserEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MyInfoEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadMyInfo()
            }
        }
    }

    private fun loadMyInfo() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getMyInfoUseCase.invoke()) {
                is AppResult.Success -> {
                    val normalizedRecentVisits = normalizeCafes(
                        items = result.data.recentVisits,
                        maxCount = result.data.recentVisits.size
                    )
                    val normalizedFavorites = normalizeCafes(
                        items = result.data.favorites,
                        maxCount = result.data.favorites.size
                    )
                    _uiState.value = MyInfoUiState(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = result.data.isLoggedIn,
                        user = result.data.user,
                        summary = result.data.summary,
                        castDetail = result.data.castDetail,
                        ownedCafes = result.data.ownedCafes,
                        badges = result.data.badges,
                        popularCafes = result.data.popularCafes,
                        recentVisits = normalizedRecentVisits,
                        favorites = normalizedFavorites,
                        followedMaids = result.data.followedMaids,
                        isLoginPromptVisible = false
                    )
                }
                is AppResult.Failure -> {
                    _uiState.value = empty().copy(
                        isLoading = false,
                        errorMessage = result.error.toString()
                    )
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CafeDetailEvent.CafeInfoUpdated -> patchCafe(event.cafe)
                    is CafeDetailEvent.FavoriteToggled -> loadMyInfo()
                    is CafeDetailEvent.MenuCreated,
                    is CafeDetailEvent.MenuUpdated,
                    is CafeDetailEvent.MenuDeleted,
                    is CafeDetailEvent.GoodsCreated,
                    is CafeDetailEvent.GoodsUpdated,
                    is CafeDetailEvent.GoodsDeleted -> Unit
                }
            }
        }
    }

    private fun observeVisitEvent() {
        jobs[TaskKey.OBSERVE_VISIT_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_VISIT_EVENT] = viewModelScope.launch {
            visitEventPublisher.events.collectLatest { event ->
                when (event) {
                    is VisitEvent.Created -> applyVisitCountDelta(1)
                    is VisitEvent.Deleted -> applyVisitCountDelta(-1)
                }
            }
        }
    }

    private fun observeUserEvent() {
        jobs[TaskKey.OBSERVE_USER_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_USER_EVENT] = viewModelScope.launch {
            userEventPublisher.events.collectLatest { event ->
                when (event) {
                    is UserEvent.ProfileUpdated -> patchUser(event.user)
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
                    is CastEvent.Updated -> {
                        val isFollowing = event.isFollowing

                        if (isFollowing != null) {
                            updateFollowedCast(event.cast, isFollowing)
                        } else {
                            patchCast(event.cast)
                        }
                    }
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeIf { it.cafe.id == cafe.id }?.copy(cafe = cafe) ?: state.castDetail,
                ownedCafes = state.ownedCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(name = cafe.name, city = cafe.region.city, rating = cafe.ratingAvg)
                    } else {
                        item
                    }
                },
                popularCafes = state.popularCafes.map { item -> if (item.id == cafe.id) cafe else item },
                recentVisits = state.recentVisits.map { item -> if (item.id == cafe.id) cafe else item },
                favorites = state.favorites.map { item -> if (item.id == cafe.id) cafe else item }
            )
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeIf { it.cast.id == cast.id }?.copy(cast = cast) ?: state.castDetail,
                followedMaids = state.followedMaids.map { item -> if (item.id == cast.id) cast else item }
            )
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeUnless { it.cast.id == castId },
                followedMaids = state.followedMaids.filterNot { it.id == castId }
            )
        }
    }

    private fun patchUser(user: User) {
        _uiState.update { state ->
            if (state.user?.id == user.id) {
                state.copy(user = user)
            } else {
                state
            }
        }
    }

    private fun updateFollowedCast(cast: Cast, isFollowing: Boolean) {
        _uiState.update { state ->
            val followedMaids = if (isFollowing) {
                upsertFollowedCast(state.followedMaids, cast)
            } else {
                state.followedMaids.filterNot { item -> item.id == cast.id }
            }
            val summary = updateFollowedCount(
                summary = state.summary,
                followedMaids = followedMaids
            )
            state.copy(
                castDetail = state.castDetail?.takeIf { detail -> detail.cast.id == cast.id }?.copy(cast = cast)
                    ?: state.castDetail,
                summary = summary,
                followedMaids = followedMaids
            )
        }
    }

    private fun upsertFollowedCast(items: List<Cast>, cast: Cast): List<Cast> {
        val hasCast = items.any { item -> item.id == cast.id }
        return if (hasCast) {
            items.map { item -> if (item.id == cast.id) cast else item }
        } else {
            listOf(cast) + items
        }
    }

    private fun updateFollowedCount(
        summary: MyPageSummary?,
        followedMaids: List<Cast>
    ): MyPageSummary? {
        return if (summary != null) {
            summary.copy(followedCastsCount = followedMaids.size)
        } else {
            null
        }
    }

    private fun applyVisitCountDelta(delta: Int) {
        if (delta == 0) return
        _uiState.update { state ->
            val summary = state.summary
            if (summary == null) {
                state
            } else {
                val nextVisitCount = max(summary.totalVisits + delta, 0)
                val nextStampCount = max(summary.badgesCount + delta, 0)
                val nextLevel = max(1, 1 + (nextVisitCount / 5))
                val nextSummary = summary.copy(
                    totalVisits = nextVisitCount,
                    badgesCount = nextStampCount,
                    level = nextLevel
                )
                val favoritesCount = nextSummary.favoritesCount
                val followedCount = nextSummary.followedCastsCount
                state.copy(
                    summary = nextSummary,
                    badges = state.badges.map { badge ->
                        val isUnlocked = when (badge.id) {
                            "badge-checkin-starter" -> nextStampCount >= 1
                            "badge-regular-visitor" -> nextVisitCount >= 5
                            "badge-favorite-curator" -> favoritesCount >= 3
                            "badge-cast-supporter" -> followedCount >= 3
                            "badge-level-up" -> nextLevel >= 3
                            "badge-concafe-master" -> nextStampCount >= 10
                            else -> badge.unlocked
                        }
                        badge.copy(unlocked = isUnlocked)
                    }
                )
            }
        }
    }

    fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.ClickCafe -> viewModelScope.launch {
                if (_uiState.value.isLoggedIn) {
                    _event.emit(NavigateToCafe(action.id))
                } else {
                    _uiState.update { it.copy(isLoginPromptVisible = true) }
                }
            }
            is MyInfoAction.ClickMaid -> viewModelScope.launch {
                if (_uiState.value.isLoggedIn) {
                    _event.emit(NavigateToCast(action.id))
                } else {
                    _uiState.update { it.copy(isLoginPromptVisible = true) }
                }
            }
            MyInfoAction.ClickLoginPromptSignIn -> viewModelScope.launch {
                _uiState.update { it.copy(isLoginPromptVisible = false) }
                _event.emit(NavigateToSignIn)
            }
            MyInfoAction.DismissLoginPrompt -> {
                _uiState.update { it.copy(isLoginPromptVisible = false) }
            }
            MyInfoAction.ClickSignIn -> viewModelScope.launch {
                _event.emit(NavigateToSignIn)
            }
            MyInfoAction.Refresh -> loadMyInfo()
        }
    }

    init {
        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        observeVisitEvent()
        observeUserEvent()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_VISIT_EVENT,
        OBSERVE_USER_EVENT
    }

    private fun normalizeCafes(items: List<Cafe>, maxCount: Int): List<Cafe> {
        val normalizedItems = items
            .distinctBy { it.id }
        return if (maxCount <= 0) {
            normalizedItems
        } else {
            normalizedItems.take(maxCount)
        }
    }
}
