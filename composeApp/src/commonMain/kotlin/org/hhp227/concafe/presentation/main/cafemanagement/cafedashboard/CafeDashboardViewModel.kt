package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CafeDashboardViewModel(
    cafeId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeDashboardUiState.preview(cafeId))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeDashboardEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateBack)
        }
    }

    private fun clickShortcut(shortcut: CafeDashboardUiState.Shortcut) {
        _uiState.update {
            it.copy(infoMessage = "${shortcut.title} 연결은 다음 단계에서 이어집니다.")
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update {
            it.copy(infoMessage = null)
        }
    }

    fun onAction(action: CafeDashboardAction) {
        when (action) {
            CafeDashboardAction.ClickBack -> clickBack()
            is CafeDashboardAction.ClickShortcut -> clickShortcut(action.shortcut)
            CafeDashboardAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }
}
