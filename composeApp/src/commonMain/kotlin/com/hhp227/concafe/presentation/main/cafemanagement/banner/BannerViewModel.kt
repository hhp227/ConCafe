package com.hhp227.concafe.presentation.main.cafemanagement.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BannerViewModel(
    private val cafeId: String? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(BannerUiState.empty())
    val uiState: StateFlow<BannerUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<BannerEvent>(replay = 0)
    val event: SharedFlow<BannerEvent> = _event.asSharedFlow()

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
}
