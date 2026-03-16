package com.hhp227.concafe.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.di.resolveSignOutUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.SignOutUseCase

class SettingsViewModel(
    private val signOutUseCase: SignOutUseCase = resolveSignOutUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<SettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickAccountSettings() {
        viewModelScope.launch {
            _event.emit(SettingsEvent.NavigateToAccountSettings)
        }
    }

    private fun clickNotificationSettings() {
        viewModelScope.launch {
            _event.emit(SettingsEvent.NavigateToNotificationSettings)
        }
    }

    private fun clickInquiry() {
        viewModelScope.launch {
            _event.emit(SettingsEvent.NavigateToInquiryLink)
        }
    }

    private fun clickPrivacyPolicy() {
        viewModelScope.launch {
            _event.emit(
                SettingsEvent.NavigateToExternalLink(
                    title = PRIVACY_POLICY_TITLE,
                    url = PRIVACY_POLICY_URL
                )
            )
        }
    }

    private fun signOut() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (signOutUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    _event.emit(SettingsEvent.NavigateBack)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "로그아웃에 실패했습니다."
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.ClickBack -> {
                viewModelScope.launch {
                    _event.emit(SettingsEvent.NavigateBack)
                }
            }
            SettingsAction.ClickAccountSettings -> clickAccountSettings()
            SettingsAction.ClickNotificationSettings -> clickNotificationSettings()
            SettingsAction.ClickCustomerSupport -> clickInquiry()
            SettingsAction.ClickInquiry -> clickInquiry()
            SettingsAction.ClickPrivacyPolicy -> clickPrivacyPolicy()
            SettingsAction.ClickSignOut -> signOut()
        }
    }

    private companion object {
        private const val PRIVACY_POLICY_TITLE = "개인정보 처리방침"
        private const val PRIVACY_POLICY_URL = "http://www.concafe.app"
    }
}
