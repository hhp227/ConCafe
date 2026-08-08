package com.hhp227.concafe.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.ObserveContentLayoutUseCase
import com.hhp227.concafe.domain.usecase.ObserveBrandThemeUseCase
import com.hhp227.concafe.domain.usecase.ObserveThemeModeUseCase
import com.hhp227.concafe.domain.usecase.SetContentLayoutUseCase
import com.hhp227.concafe.domain.usecase.SetBrandThemeUseCase
import com.hhp227.concafe.domain.usecase.SetThemeModeUseCase
import com.hhp227.concafe.domain.usecase.SignOutUseCase
import com.hhp227.concafe.presentation.theme.AppContentLayout
import com.hhp227.concafe.presentation.theme.AppBrandTheme
import com.hhp227.concafe.presentation.theme.AppThemeMode
import com.hhp227.concafe.presentation.theme.toDomainContentLayout
import com.hhp227.concafe.presentation.theme.toDomainBrandTheme
import com.hhp227.concafe.presentation.theme.toDomainThemeMode
import com.hhp227.concafe.presentation.theme.toPresentationContentLayout
import com.hhp227.concafe.presentation.theme.toPresentationBrandTheme
import com.hhp227.concafe.presentation.theme.toPresentationThemeMode
import kotlinx.coroutines.Job

class SettingsViewModel(
    private val signOutUseCase: SignOutUseCase,
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val observeBrandThemeUseCase: ObserveBrandThemeUseCase,
    private val setBrandThemeUseCase: SetBrandThemeUseCase,
    private val observeContentLayoutUseCase: ObserveContentLayoutUseCase,
    private val setContentLayoutUseCase: SetContentLayoutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<SettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

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

        jobs[JobKey.SIGN_OUT]?.cancel()
        jobs[JobKey.SIGN_OUT] = viewModelScope.launch {
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

    private fun selectThemeMode(themeMode: AppThemeMode) {
        setThemeModeUseCase.invoke(themeMode.toDomainThemeMode())
        _uiState.update { it.copy(themeMode = themeMode) }
    }

    private fun observeThemeMode() {
        jobs[JobKey.OBSERVE_THEME]?.cancel()
        jobs[JobKey.OBSERVE_THEME] = viewModelScope.launch {
            observeThemeModeUseCase.invoke().collect { themeMode ->
                _uiState.update { it.copy(themeMode = themeMode.toPresentationThemeMode()) }
            }
        }
    }

    private fun selectBrandTheme(brandTheme: AppBrandTheme) {
        setBrandThemeUseCase.invoke(brandTheme.toDomainBrandTheme())
        _uiState.update { it.copy(brandTheme = brandTheme) }
    }

    private fun observeBrandTheme() {
        jobs[JobKey.OBSERVE_BRAND_THEME]?.cancel()
        jobs[JobKey.OBSERVE_BRAND_THEME] = viewModelScope.launch {
            observeBrandThemeUseCase.invoke().collect { brandTheme ->
                _uiState.update { it.copy(brandTheme = brandTheme.toPresentationBrandTheme()) }
            }
        }
    }

    private fun selectContentLayout(contentLayout: AppContentLayout) {
        setContentLayoutUseCase.invoke(contentLayout.toDomainContentLayout())
        _uiState.update { it.copy(contentLayout = contentLayout) }
    }

    private fun observeContentLayout() {
        jobs[JobKey.OBSERVE_CONTENT_LAYOUT]?.cancel()
        jobs[JobKey.OBSERVE_CONTENT_LAYOUT] = viewModelScope.launch {
            observeContentLayoutUseCase.invoke().collect { contentLayout ->
                _uiState.update { it.copy(contentLayout = contentLayout.toPresentationContentLayout()) }
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
            is SettingsAction.SelectThemeMode -> selectThemeMode(action.themeMode)
            is SettingsAction.SelectBrandTheme -> selectBrandTheme(action.brandTheme)
            is SettingsAction.SelectContentLayout -> selectContentLayout(action.contentLayout)
        }
    }

    init {
        observeThemeMode()
        observeBrandTheme()
        observeContentLayout()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class JobKey {
        SIGN_OUT,
        OBSERVE_THEME,
        OBSERVE_BRAND_THEME,
        OBSERVE_CONTENT_LAYOUT
    }

    private companion object {
        private const val PRIVACY_POLICY_TITLE = "개인정보 처리방침"
        private const val PRIVACY_POLICY_URL = "https://concafe-5f7fd.firebaseapp.com/privacy"
    }
}
