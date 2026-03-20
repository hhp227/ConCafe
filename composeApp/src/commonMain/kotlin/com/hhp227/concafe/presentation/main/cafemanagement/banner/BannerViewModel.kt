package com.hhp227.concafe.presentation.main.cafemanagement.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.BannerEvent as BannerDomainEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.usecase.DeleteHomeBannerUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BannerViewModel(
    private val cafeId: String? = null,
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val bannerEventPublisher: BannerEventPublisher,
    private val deleteHomeBannerUseCase: DeleteHomeBannerUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BannerUiState.empty())
    val uiState: StateFlow<BannerUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<BannerEvent>(replay = 0)
    val event: SharedFlow<BannerEvent> = _event.asSharedFlow()

    private fun loadBanners() {
        viewModelScope.launch {
            when (val result = getHomeFeedUseCase.invoke(popularCastCursor = null, nearbyCafeCursor = null)) {
                is AppResult.Success -> {
                    val targetCafeId = cafeId
                    val mapped = result.data.banners
                        .asSequence()
                        .filter { banner -> targetCafeId.isNullOrBlank() || banner.cafeId == targetCafeId }
                        .map { banner -> banner.toBannerItem() }
                        .toList()
                    _uiState.update { state ->
                        state.copy(
                            banners = mapped,
                            pendingDeleteBannerId = state.pendingDeleteBannerId?.takeIf { pendingId ->
                                mapped.any { it.id == pendingId }
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _event.emit(BannerEvent.ShowMessage("배너 목록을 불러오지 못했습니다."))
                }
            }
        }
    }

    private fun observeBannerEvent() {
        viewModelScope.launch {
            bannerEventPublisher.events.collectLatest { event ->
                when (event) {
                    is BannerDomainEvent.Created -> loadBanners()
                    is BannerDomainEvent.Updated -> patchBanner(event.banner)
                    is BannerDomainEvent.Deleted -> removeBanner(event.banner.id)
                }
            }
        }
    }

    private fun patchBanner(updatedBanner: HomeBanner) {
        _uiState.update { state ->
            val targetCafeId = cafeId
            val shouldShow = targetCafeId.isNullOrBlank() || updatedBanner.cafeId == targetCafeId
            val existingIndex = state.banners.indexOfFirst { it.id == updatedBanner.id }

            when {
                shouldShow && existingIndex >= 0 -> {
                    val patched = state.banners.toMutableList()
                    patched[existingIndex] = updatedBanner.toBannerItem()
                    state.copy(banners = patched)
                }
                shouldShow && existingIndex < 0 -> {
                    state.copy(banners = listOf(updatedBanner.toBannerItem()) + state.banners)
                }
                !shouldShow && existingIndex >= 0 -> {
                    val filtered = state.banners.filterNot { it.id == updatedBanner.id }
                    state.copy(
                        banners = filtered,
                        pendingDeleteBannerId = state.pendingDeleteBannerId?.takeIf { pendingId ->
                            filtered.any { it.id == pendingId }
                        }
                    )
                }
                else -> state
            }
        }
    }

    private fun removeBanner(bannerId: String) {
        _uiState.update { state ->
            state.copy(
                banners = state.banners.filterNot { it.id == bannerId },
                pendingDeleteBannerId = state.pendingDeleteBannerId?.takeUnless { it == bannerId }
            )
        }
    }

    private fun clickDeleteBanner(bannerId: String) {
        val hasBanner = _uiState.value.banners.any { it.id == bannerId }
        if (!hasBanner) {
            return
        }
        _uiState.update { it.copy(pendingDeleteBannerId = bannerId) }
    }

    private fun dismissDeleteBannerDialog() {
        _uiState.update { it.copy(pendingDeleteBannerId = null) }
    }

    private fun confirmDeleteBanner() {
        val bannerId = _uiState.value.pendingDeleteBannerId
        if (bannerId == null) {
            return
        }
        viewModelScope.launch {
            when (val result = deleteHomeBannerUseCase.invoke(bannerId)) {
                is AppResult.Success -> {
                    removeBanner(bannerId)
                    _event.emit(BannerEvent.ShowMessage("배너를 삭제했습니다."))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(pendingDeleteBannerId = null) }
                    _event.emit(BannerEvent.ShowMessage(result.error.toString()))
                }
            }
        }
    }

    fun onAction(action: BannerAction) {
        when (action) {
            BannerAction.ClickBack -> emitEvent(BannerEvent.NavigateBack)
            BannerAction.ClickCreateBanner -> emitEvent(BannerEvent.NavigateToBannerEdit(cafeId = cafeId))
            BannerAction.ConfirmDeleteBanner -> confirmDeleteBanner()
            BannerAction.DismissDeleteBannerDialog -> dismissDeleteBannerDialog()
            is BannerAction.ClickDeleteBanner -> clickDeleteBanner(action.bannerId)
            is BannerAction.ClickEditBanner -> clickEditBanner(action.bannerId)
            is BannerAction.SelectTab -> _uiState.update { it.copy(selectedTab = action.tab) }
        }
    }

    private fun clickEditBanner(bannerId: String) {
        val banner = _uiState.value.banners.firstOrNull { it.id == bannerId } ?: return
        emitEvent(
            BannerEvent.NavigateToBannerEdit(
                cafeId = banner.cafeId ?: cafeId,
                bannerId = banner.id
            )
        )
    }

    private fun emitEvent(event: BannerEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    init {
        observeBannerEvent()
        loadBanners()
    }
}

private fun HomeBanner.toBannerItem(): BannerItem {
    val tab = when (statusLabel.uppercase()) {
        "SCHEDULED" -> BannerTab.SCHEDULED
        "ENDED", "PAUSED" -> BannerTab.ENDED
        else -> BannerTab.ACTIVE
    }
    val status = when (tab) {
        BannerTab.ACTIVE -> "진행 중"
        BannerTab.SCHEDULED -> "예약"
        BannerTab.ENDED -> "종료"
    }
    val icon = when (targetType) {
        BannerLinkTargetType.CAFE_DETAIL -> "local_cafe"
        BannerLinkTargetType.EVENT_DETAIL -> "event"
        BannerLinkTargetType.NOTICE -> "campaign"
        BannerLinkTargetType.EXTERNAL_LINK -> "language"
    }
    return BannerItem(
        id = id,
        cafeId = cafeId,
        title = title,
        description = subtitle,
        periodText = "노출 ${displayDays}일",
        statusLabel = status,
        tab = tab,
        accentColorHex = startColorHex,
        imageIcon = icon
    )
}
