package com.hhp227.concafe.presentation.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.usecase.GetNotificationSettingsUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.UpdateNotificationSettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState.initial())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NotificationSettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(NotificationSettingsEvent.NavigateBack)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getNotificationSettingsUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSaving = false,
                            errorMessage = null,
                            isPushNotificationsEnabled = result.data.isPushNotificationsEnabled,
                            isShiftNotificationsEnabled = result.data.isShiftNotificationsEnabled,
                            isBirthdayNotificationsEnabled = result.data.isBirthdayNotificationsEnabled,
                            isNoticeNotificationsEnabled = result.data.isNoticeNotificationsEnabled,
                            isFollowNotificationsEnabled = result.data.isFollowNotificationsEnabled,
                            isEventNotificationsEnabled = result.data.isEventNotificationsEnabled,
                            quietHoursOption = result.data.quietHoursMode
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSaving = false,
                            errorMessage = result.error.toMessage()
                        )
                    }
                }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update { state ->
                    state.copy(isCastRole = user?.role == UserRole.CAST)
                }
            }
        }
    }

    private fun updateSettings(update: (NotificationSettingsUiState) -> NotificationSettingsUiState) {
        val currentState = _uiState.value
        val nextState = update(currentState)
        _uiState.value = nextState

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val settings = UserNotificationSettings(
                isPushNotificationsEnabled = nextState.isPushNotificationsEnabled,
                isShiftNotificationsEnabled = nextState.isShiftNotificationsEnabled,
                isBirthdayNotificationsEnabled = nextState.isBirthdayNotificationsEnabled,
                isNoticeNotificationsEnabled = nextState.isNoticeNotificationsEnabled,
                isFollowNotificationsEnabled = nextState.isFollowNotificationsEnabled,
                isEventNotificationsEnabled = nextState.isEventNotificationsEnabled,
                quietHoursMode = nextState.quietHoursOption
            )
            when (val result = updateNotificationSettingsUseCase.invoke(settings)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            errorMessage = null,
                            isPushNotificationsEnabled = result.data.isPushNotificationsEnabled,
                            isShiftNotificationsEnabled = result.data.isShiftNotificationsEnabled,
                            isBirthdayNotificationsEnabled = result.data.isBirthdayNotificationsEnabled,
                            isNoticeNotificationsEnabled = result.data.isNoticeNotificationsEnabled,
                            isFollowNotificationsEnabled = result.data.isFollowNotificationsEnabled,
                            isEventNotificationsEnabled = result.data.isEventNotificationsEnabled,
                            quietHoursOption = result.data.quietHoursMode
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isSaving = false,
                        errorMessage = result.error.toMessage()
                    )
                    _event.emit(NotificationSettingsEvent.ShowMessage(result.error.toMessage()))
                }
            }
        }
    }

    fun onAction(action: NotificationSettingsAction) {
        when (action) {
            NotificationSettingsAction.ClickBack -> clickBack()
            is NotificationSettingsAction.TogglePushNotifications -> {
                updateSettings { it.copy(isPushNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.ToggleShiftNotifications -> {
                updateSettings { it.copy(isShiftNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.ToggleBirthdayNotifications -> {
                updateSettings { it.copy(isBirthdayNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.ToggleNoticeNotifications -> {
                updateSettings { it.copy(isNoticeNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.ToggleFollowNotifications -> {
                updateSettings { it.copy(isFollowNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.ToggleEventNotifications -> {
                updateSettings { it.copy(isEventNotificationsEnabled = action.enabled) }
            }
            is NotificationSettingsAction.SelectQuietHours -> {
                updateSettings { it.copy(quietHoursOption = action.option) }
            }
        }
    }

    init {
        observeSession()
        loadSettings()
    }
}

private fun AppError.toMessage(): String {
    return when (this) {
        AppError.Unauthorized -> "로그인 후 알림 설정을 변경해 주세요."
        AppError.PermissionDenied -> "알림 설정 변경 권한이 없습니다."
        AppError.NotFound -> "알림 설정 데이터를 찾을 수 없습니다."
        is AppError.ValidationFailed -> this.reason
        is AppError.NetworkError -> this.message ?: "네트워크 오류가 발생했습니다."
        is AppError.Unknown -> this.cause ?: "알림 설정 저장에 실패했습니다."
    }
}
