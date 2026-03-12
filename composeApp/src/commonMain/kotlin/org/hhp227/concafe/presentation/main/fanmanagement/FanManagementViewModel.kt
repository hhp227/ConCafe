package org.hhp227.concafe.presentation.main.fanmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FanManagementViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FanManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<FanManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun showEventMessage(message: String) {
        viewModelScope.launch {
            _event.emit(FanManagementEvent.ShowMessage(message))
        }
    }

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun clickQuickAction(quickAction: FanManagementUiState.QuickAction) {
        when (quickAction) {
            FanManagementUiState.QuickAction.WORK_SCHEDULE -> {
                setInfoMessage("출근 관리 화면 연결은 다음 단계에서 구현합니다.")
            }
        }
    }

    private fun clickRecentFollower(followerId: String) {
        val follower = uiState.value.recentFollowers.firstOrNull { it.id == followerId } ?: return
        setInfoMessage("${follower.name} 팬 상세 화면은 다음 단계에서 연결합니다.")
    }

    private fun clickTopFan(fanId: String) {
        val fan = uiState.value.topFans.firstOrNull { it.id == fanId } ?: return
        setInfoMessage("${fan.name} 활동 리포트는 다음 단계에서 제공합니다.")
    }

    fun onAction(action: FanManagementAction) {
        when (action) {
            FanManagementAction.ClickEditProfile -> {
                val castProfile = uiState.value.castProfile
                viewModelScope.launch {
                    _event.emit(
                        FanManagementEvent.NavigateToCastEdit(
                            cafeId = castProfile.cafeId,
                            castId = castProfile.castId
                        )
                    )
                }
            }
            FanManagementAction.ClickNotification -> {
                showEventMessage("새 알림 ${uiState.value.notificationCount}건이 있습니다.")
            }
            FanManagementAction.ClickPrimaryAnnouncement -> {
                setInfoMessage("팬 공지 작성 흐름은 다음 단계에서 연결합니다.")
            }
            is FanManagementAction.ClickQuickAction -> clickQuickAction(action.quickAction)
            FanManagementAction.ClickViewAllFollowers -> {
                setInfoMessage("전체 팔로워 목록은 다음 단계에서 제공합니다.")
            }
            is FanManagementAction.ClickRecentFollower -> clickRecentFollower(action.followerId)
            is FanManagementAction.ClickTopFan -> clickTopFan(action.fanId)
            FanManagementAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }
}
