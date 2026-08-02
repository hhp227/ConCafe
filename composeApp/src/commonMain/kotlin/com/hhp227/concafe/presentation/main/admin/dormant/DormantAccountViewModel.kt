package com.hhp227.concafe.presentation.main.admin.dormant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.usecase.GetDormantAccountPageUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserDormantStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DormantAccountViewModel(
    private val getDormantAccountPageUseCase: GetDormantAccountPageUseCase,
    private val updateUserDormantStatusUseCase: UpdateUserDormantStatusUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DormantAccountUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<DormantAccountEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var pageJob: Job? = null

    fun onAction(action: DormantAccountAction) {
        when (action) {
            is DormantAccountAction.SelectFilter -> {
                if (_uiState.value.selectedFilter != action.filter) {
                    _uiState.update {
                        it.copy(
                            selectedFilter = action.filter,
                            users = emptyList(),
                            nextCursor = null,
                            canLoadMore = false,
                            confirmTarget = null,
                            infoMessage = null
                        )
                    }
                    loadPage(cursor = null, append = false)
                }
            }
            DormantAccountAction.LoadMore -> loadMore()
            is DormantAccountAction.RequestDormantChange -> {
                _uiState.update {
                    it.copy(confirmTarget = DormantChangeRequest(user = action.user, dormant = action.dormant))
                }
            }
            DormantAccountAction.ConfirmDormantChange -> confirmDormantChange()
            DormantAccountAction.CancelDormantChange -> {
                _uiState.update { it.copy(confirmTarget = null) }
            }
            DormantAccountAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        val cursor = state.nextCursor
        if (cursor == null || !state.canLoadMore || state.isLoadingMore) return
        loadPage(cursor = cursor, append = true)
    }

    private fun loadPage(cursor: String?, append: Boolean) {
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            if (append) {
                _uiState.update { it.copy(isLoadingMore = true) }
                delay(PAGINATION_DELAY_MILLIS)
            } else {
                _uiState.update { it.copy(isLoading = true, infoMessage = null) }
            }

            when (val result = getDormantAccountPageUseCase.invoke(
                filter = _uiState.value.selectedFilter,
                cursor = cursor,
                pageSize = DORMANT_ACCOUNT_PAGE_SIZE
            )) {
                is AppResult.Success -> {
                    val pageItems = if (_uiState.value.selectedFilter == DormantAccountFilter.DORMANT) {
                        result.data.items.sortedByDescending { it.createdAt }
                    } else {
                        result.data.items.sortedBy { it.lastLoginAt }
                    }
                    _uiState.update { state ->
                        state.copy(
                            users = if (append) state.users + pageItems else pageItems,
                            nextCursor = result.data.nextCursor,
                            canLoadMore = result.data.hasNext,
                            isLoading = false,
                            isLoadingMore = false,
                            infoMessage = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun confirmDormantChange() {
        val request = _uiState.value.confirmTarget

        if (request == null || _uiState.value.isUpdating) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }

            when (val result = updateUserDormantStatusUseCase.invoke(
                userId = request.user.id,
                dormant = request.dormant
            )) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            users = state.users.filterNot { it.id == request.user.id },
                            isUpdating = false,
                            confirmTarget = null,
                            infoMessage = if (request.dormant) {
                                "${request.user.nickname} 계정을 휴면 전환했습니다."
                            } else {
                                "${request.user.nickname} 계정의 휴면을 해제했습니다."
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            confirmTarget = null,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        pageJob?.cancel()
        super.onCleared()
    }

    init {
        loadPage(cursor = null, append = false)
    }
}

private const val DORMANT_ACCOUNT_PAGE_SIZE = 15
private const val PAGINATION_DELAY_MILLIS = 1_000L
