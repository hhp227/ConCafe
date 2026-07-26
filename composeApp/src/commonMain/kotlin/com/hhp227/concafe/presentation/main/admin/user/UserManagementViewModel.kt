package com.hhp227.concafe.presentation.main.admin.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetAdminUserPageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserManagementViewModel(
    private val getAdminUserPageUseCase: GetAdminUserPageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<UserManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var pageJob: Job? = null

    fun onAction(action: UserManagementAction) {
        when (action) {
            is UserManagementAction.SelectFilter -> {
                if (_uiState.value.selectedFilter != action.filter) {
                    _uiState.update {
                        it.copy(
                            selectedFilter = action.filter,
                            users = emptyList(),
                            nextCursor = null,
                            canLoadMore = false,
                            infoMessage = null
                        )
                    }
                    loadPage(cursor = null, append = false)
                }
            }
            UserManagementAction.LoadMore -> loadMore()
            UserManagementAction.DismissInfoMessage -> {
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

            when (val result = getAdminUserPageUseCase.invoke(
                filter = _uiState.value.selectedFilter,
                cursor = cursor,
                pageSize = USER_PAGE_SIZE
            )) {
                is AppResult.Success -> {
                    val pageItems = result.data.items.sortedByDescending { it.createdAt }
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

    override fun onCleared() {
        pageJob?.cancel()
        super.onCleared()
    }

    init {
        loadPage(cursor = null, append = false)
    }
}

private const val USER_PAGE_SIZE = 15
private const val PAGINATION_DELAY_MILLIS = 1_000L
