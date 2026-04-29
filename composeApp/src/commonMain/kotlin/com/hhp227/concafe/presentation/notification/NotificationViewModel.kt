package com.hhp227.concafe.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import com.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class NotificationViewModel(
    private val getNotificationFeedUseCase: GetNotificationFeedUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NotificationEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadNotifications()
            }
        }
    }

    private fun loadNotifications() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getNotificationFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.value = NotificationUiState(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = result.data.isLoggedIn,
                        unreadCount = result.data.unreadCount,
                        sections = result.data.sections
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "알림을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun handleNotificationClick(id: String, type: String, targetId: String?) {
        viewModelScope.launch {
            when (markNotificationReadUseCase.invoke(id)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val updatedSections = state.sections.map { section ->
                            section.copy(
                                items = section.items.map { item ->
                                    if (item.id == id) {
                                        item.copy(isRead = true)
                                    } else {
                                        item
                                    }
                                }
                            )
                        }
                        val unreadCount = updatedSections.sumOf { section ->
                            section.items.count { !it.isRead }
                        }

                        state.copy(
                            unreadCount = unreadCount,
                            sections = updatedSections
                        )
                    }
                    if (targetId != null) {
                        when (type) {
                            "CAST_SHIFT", "BIRTHDAY" -> _event.emit(NotificationEvent.NavigateToCast(targetId))
                            "CAFE_NOTICE", "CAFE_EVENT", "CAFE_TABLE_COUNT_UPDATE" -> _event.emit(NotificationEvent.NavigateToCafe(targetId))
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = "알림 상태를 업데이트하지 못했습니다.") }
                }
            }
        }
    }

    fun onAction(action: NotificationAction) {
        when (action) {
            NotificationAction.ClickBack -> {
                viewModelScope.launch {
                    _event.emit(NotificationEvent.NavigateBack)
                }
            }
            is NotificationAction.ClickNotification -> handleNotificationClick(action.id, action.type, action.targetId)
            NotificationAction.ClickSignIn -> {
                viewModelScope.launch {
                    _event.emit(NotificationEvent.NavigateToSignIn)
                }
            }
            NotificationAction.Refresh -> loadNotifications()
        }
    }

    init {
        observeSession()
        loadNotifications()
    }
}
