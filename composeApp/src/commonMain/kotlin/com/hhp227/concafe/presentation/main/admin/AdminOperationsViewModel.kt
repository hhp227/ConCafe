package com.hhp227.concafe.presentation.main.admin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AdminOperationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminOperationsUiState())
    val uiState: StateFlow<AdminOperationsUiState> = _uiState.asStateFlow()

    private fun handlePendingResult(id: String, approved: Boolean) {
        val request = _uiState.value.pendingRequests.firstOrNull { it.id == id } ?: return
        _uiState.update { state ->
            state.copy(
                pendingRequests = state.pendingRequests.filterNot { it.id == id },
                infoMessage = if (approved) {
                    "${request.title} 요청을 승인했습니다."
                } else {
                    "${request.title} 요청을 반려했습니다."
                }
            )
        }
    }

    fun onAction(action: AdminOperationsAction) {
        when (action) {
            AdminOperationsAction.ClickNotifications -> {
                _uiState.update {
                    it.copy(hasUnreadNotifications = false, infoMessage = "새 알림을 모두 확인했습니다.")
                }
            }
            AdminOperationsAction.ClickSeeAllPending -> {
                _uiState.update { it.copy(infoMessage = "전체보기 연결은 다음 단계에서 이어집니다.") }
            }
            is AdminOperationsAction.SelectPendingFilter -> {
                _uiState.update { it.copy(selectedPendingFilter = action.filter, infoMessage = null) }
            }
            is AdminOperationsAction.ApprovePending -> {
                handlePendingResult(action.id, approved = true)
            }
            is AdminOperationsAction.RejectPending -> {
                handlePendingResult(action.id, approved = false)
            }
            is AdminOperationsAction.ClickQuickMenu -> {
                val label = _uiState.value.quickMenus.firstOrNull { it.id == action.id }?.title ?: "메뉴"
                _uiState.update { it.copy(infoMessage = "$label 연결은 다음 단계에서 이어집니다.") }
            }
            AdminOperationsAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }
}
