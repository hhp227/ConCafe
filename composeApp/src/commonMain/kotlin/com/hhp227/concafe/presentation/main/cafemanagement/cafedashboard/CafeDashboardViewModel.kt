package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

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
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class CafeDashboardViewModel(
    private val cafeId: String,
    private val getCafeCastPageUseCase: GetCafeCastPageUseCase,
    private val getCafeDashboardUseCase: GetCafeDashboardUseCase,
    private val observeCafeCastVersionUseCase: ObserveCafeCastVersionUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeDashboardEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadCafeDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }

            when (val result = getCafeDashboardUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafe = result.data,
                            isLoading = false
                        )
                    }
                    refreshCastPreviews(resetMessage = false)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            cafe = null,
                            castPreviews = emptyList(),
                            nextCastCursor = null,
                            hasMoreCasts = false,
                            isLoading = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun loadCastPage(cursor: String?, pageSize: Int, append: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMoreCasts = append,
                    infoMessage = if (append) it.infoMessage else null
                )
            }

            when (val result = getCafeCastPageUseCase.invoke(cafeId, cursor, pageSize)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val mergedItems = if (append) state.castPreviews + result.data.items else result.data.items
                        val nextSelectedCastId = state.selectedCastId?.takeIf { selectedId ->
                            mergedItems.any { it.id == selectedId }
                        }
                        state.copy(
                            castPreviews = mergedItems,
                            selectedCastId = nextSelectedCastId,
                            nextCastCursor = result.data.nextCursor,
                            hasMoreCasts = result.data.hasNext,
                            isLoadingMoreCasts = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMoreCasts = false,
                            infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun refreshCastPreviews(resetMessage: Boolean = true) {
        if (resetMessage) {
            _uiState.update { it.copy(infoMessage = null) }
        }
        loadCastPage(
            cursor = null,
            pageSize = getCafeCastPageUseCase.defaultPageSize(),
            append = false
        )
    }

    private fun clickLoadMoreCasts() {
        val currentState = _uiState.value

        if (currentState.isLoadingMoreCasts || !currentState.hasMoreCasts) return
        loadCastPage(
            cursor = currentState.nextCastCursor,
            pageSize = getCafeCastPageUseCase.defaultPageSize(),
            append = true
        )
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateBack)
        }
    }

    private fun clickShortcut(shortcut: CafeDashboardShortcut) {
        when (shortcut) {
            CafeDashboardShortcut.CAFE_SETTINGS -> {
                viewModelScope.launch {
                    _event.emit(CafeDashboardEvent.NavigateToCafeInfoEdit(cafeId))
                }
            }
            CafeDashboardShortcut.MENU_GOODS -> {
                viewModelScope.launch {
                    _event.emit(CafeDashboardEvent.NavigateToMenuGoods(cafeId))
                }
            }
            CafeDashboardShortcut.CAST_MANAGEMENT -> {
                viewModelScope.launch {
                    _event.emit(CafeDashboardEvent.NavigateToCastEdit(cafeId = cafeId))
                }
            }
            CafeDashboardShortcut.CAST_SCHEDULE -> {
                val selectedCastId = _uiState.value.selectedCastId
                if (selectedCastId == null) {
                    _uiState.update {
                        it.copy(infoMessage = "출근표를 관리할 캐스트를 목록에서 선택해 주세요.")
                    }
                } else {
                    viewModelScope.launch {
                        _event.emit(CafeDashboardEvent.NavigateToSchedule(selectedCastId))
                    }
                }
            }
            else -> {
                _uiState.update {
                    it.copy(infoMessage = "${shortcut.title} 연결은 다음 단계에서 이어집니다.")
                }
            }
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update {
            it.copy(infoMessage = null)
        }
    }

    private fun clickCastSchedule(castId: String) {
        _uiState.update {
            it.copy(
                selectedCastId = if (it.selectedCastId == castId) null else castId,
                infoMessage = null
            )
        }
    }

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadCafeDashboard()
            }
        }
    }

    private fun observeCastVersion() {
        jobs[TaskKey.OBSERVE_CAST_VERSION]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_VERSION] = viewModelScope.launch {
            observeCafeCastVersionUseCase.invoke(cafeId).collectLatest {
                refreshCastPreviews()
            }
        }
    }

    fun onAction(action: CafeDashboardAction) {
        when (action) {
            CafeDashboardAction.ClickBack -> clickBack()
            is CafeDashboardAction.ClickShortcut -> clickShortcut(action.shortcut)
            is CafeDashboardAction.ClickCastSchedule -> clickCastSchedule(action.castId)
            CafeDashboardAction.ClickLoadMoreCasts -> clickLoadMoreCasts()
            CafeDashboardAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeSession()
        observeCastVersion()
        loadCafeDashboard()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_SESSION,
        OBSERVE_CAST_VERSION
    }
}
