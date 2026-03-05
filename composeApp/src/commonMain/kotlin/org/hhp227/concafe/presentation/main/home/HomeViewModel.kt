package org.hhp227.concafe.presentation.main.home

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(HomeUiState.empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event = _event.asSharedFlow()

    val action: (HomeAction) -> Unit = ::onAction

    private fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            val result = getHomeFeedUseCase.invoke(limit = 10)

            if (result is AppResult.Success) {
                _uiState.value = mapHomeFeedToState(result.data)
            } else if (result is AppResult.Failure) {
                _uiState.value = HomeUiState.empty().copy(
                    isLoading = false,
                    errorMessage = result.error.toString()
                )
            }
        }
    }

    private fun mapHomeFeedToState(feed: HomeFeed): HomeUiState {
        val banners = feed.banners.map {
            HomeBannerUi(
                id = it.id,
                title = it.title,
                gradient = listOf(
                    colorFromHex(it.startColorHex),
                    colorFromHex(it.endColorHex)
                )
            )
        }

        val popularMaids = feed.popularCasts.map {
            PopularMaidUi(
                id = it.id,
                name = it.name,
                cafe = it.cafeName,
                followers = it.followers
            )
        }

        val nearbyCafes = feed.nearbyCafes.map {
            NearbyCafeUi(
                id = it.id,
                name = it.name,
                rating = it.rating.toString(),
                location = it.location,
                distance = it.distance
            )
        }

        val birthdayMaids = feed.birthdayCasts.map {
            BirthdayMaidUi(
                id = it.id,
                name = it.name
            )
        }

        val notices = feed.notices.map {
            NoticeUi(
                id = it.id,
                cafe = it.cafeName,
                content = it.content,
                time = it.relativeTime
            )
        }

        return HomeUiState(
            isLoading = false,
            errorMessage = null,
            banners = banners,
            popularMaids = popularMaids,
            nearbyCafes = nearbyCafes,
            birthdayMaids = birthdayMaids,
            notices = notices
        )
    }

    private fun colorFromHex(hex: String): androidx.compose.ui.graphics.Color {
        val normalized = hex.removePrefix("#")
        val value = normalized.toLongOrNull(16) ?: return androidx.compose.ui.graphics.Color.Gray
        return androidx.compose.ui.graphics.Color(
            red = ((value shr 16) and 0xFF).toInt(),
            green = ((value shr 8) and 0xFF).toInt(),
            blue = (value and 0xFF).toInt()
        )
    }

    private fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.ClickMaid -> _event.tryEmit(HomeEvent.NavigateToCastDetail(action.id))
            is HomeAction.ClickBirthdayMaid -> _event.tryEmit(HomeEvent.NavigateToCastDetail(action.id))
            is HomeAction.ClickCafe -> _event.tryEmit(HomeEvent.NavigateToCafeDetail(action.id))
        }
    }

    init {
        loadHomeFeed()
    }

    override fun onCleared() {
        super.onCleared()
        scope.coroutineContext.cancel()
    }
}
