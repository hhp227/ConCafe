package org.hhp227.concafe.presentation.main.cafemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CafeManagementViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CafeManagementUiState.preview())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickCafe(cafeId: String) {
        viewModelScope.launch {
            _event.emit(CafeManagementEvent.NavigateToCafeDashboard(cafeId))
        }
    }

    private fun clickCafeDetail(cafeId: String) {
        viewModelScope.launch {
            _event.emit(CafeManagementEvent.NavigateToCafe(cafeId))
        }
    }

    private fun changeCafeSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                cafeSearchQuery = query,
                infoMessage = null
            )
        }
    }

    private fun clickClaimCafe(cafeId: String) {
        val cafeName = _uiState.value.searchableCafes.firstOrNull { it.id == cafeId }?.name ?: "선택한 카페"

        _uiState.update {
            it.copy(
                infoMessage = "$cafeName 운영자 신청 연결은 다음 단계에서 이어집니다."
            )
        }
    }

    private fun toggleCafeListExpanded() {
        _uiState.update {
            it.copy(
                isShowingAllCafes = !it.isShowingAllCafes
            )
        }
    }

    private fun clickCreateCafe() {
        _uiState.update {
            it.copy(
                infoMessage = "새 카페 등록 플로우는 다음 단계에서 연결됩니다."
            )
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update {
            it.copy(
                infoMessage = null
            )
        }
    }

    fun onAction(action: CafeManagementAction) {
        when (action) {
            is CafeManagementAction.ClickCafe -> clickCafe(action.cafeId)
            is CafeManagementAction.ClickCafeDetail -> clickCafeDetail(action.cafeId)
            is CafeManagementAction.ChangeCafeSearchQuery -> changeCafeSearchQuery(action.query)
            is CafeManagementAction.ClickClaimCafe -> clickClaimCafe(action.cafeId)
            CafeManagementAction.ToggleCafeListExpanded -> toggleCafeListExpanded()
            CafeManagementAction.ClickCreateCafe -> clickCreateCafe()
            CafeManagementAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }
}
