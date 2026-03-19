package com.hhp227.concafe.presentation.main.cafemanagement.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.BannerEvent as BannerDomainEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBanner
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
    private val bannerEventPublisher: BannerEventPublisher
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
                    _uiState.update { it.copy(banners = mapped) }
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
                }
            }
        }
    }

    fun onAction(action: BannerAction) {
        when (action) {
            BannerAction.ClickBack -> emitEvent(BannerEvent.NavigateBack)
            BannerAction.ClickCreateBanner -> emitEvent(BannerEvent.NavigateToBannerEdit(cafeId))
            is BannerAction.ClickDeleteBanner -> emitEvent(BannerEvent.ShowMessage("삭제 기능은 아직 연결되지 않았습니다."))
            is BannerAction.ClickEditBanner -> emitEvent(BannerEvent.ShowMessage("편집 기능은 아직 연결되지 않았습니다."))
            is BannerAction.SelectTab -> _uiState.update { it.copy(selectedTab = action.tab) }
        }
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
        title = title,
        description = subtitle,
        periodText = "노출 ${displayDays}일",
        statusLabel = status,
        tab = tab,
        accentColorHex = startColorHex,
        imageIcon = icon
    )
}
