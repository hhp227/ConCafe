package com.hhp227.concafe.presentation.main.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.usecase.ApproveCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.ApproveCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeOwnerClaimsUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeRegistrationClaimsUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeRegistrationClaimUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminOperationsViewModel(
    private val getPendingCafeRegistrationClaimsUseCase: GetPendingCafeRegistrationClaimsUseCase,
    private val getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase,
    private val approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase,
    private val approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase,
    private val rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase,
    private val rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminOperationsUiState())
    val uiState: StateFlow<AdminOperationsUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminOperationsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadPendingRequests() {
        viewModelScope.launch {
            val registrationResult = getPendingCafeRegistrationClaimsUseCase.invoke()
            val ownerClaimResult = getPendingCafeOwnerClaimsUseCase.invoke()

            when {
                registrationResult is AppResult.Success && ownerClaimResult is AppResult.Success -> {
                    val registrationClaims = registrationResult.data.sortedByDescending { it.requestedAt }
                    val ownerClaims = ownerClaimResult.data.sortedByDescending { it.requestedAt }
                    val pendingCount = registrationClaims.size + ownerClaims.size
                    _uiState.update { state ->
                        state.copy(
                            pendingCafeRegistrationClaims = registrationClaims,
                            pendingCafeOwnerClaims = ownerClaims,
                            metrics = buildAdminMetrics(pendingCount),
                            infoMessage = null
                        )
                    }
                }
                registrationResult is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            pendingCafeRegistrationClaims = emptyList(),
                            pendingCafeOwnerClaims = emptyList(),
                            metrics = buildAdminMetrics(0),
                            infoMessage = registrationResult.error.toString()
                        )
                    }
                }
                ownerClaimResult is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            pendingCafeRegistrationClaims = emptyList(),
                            pendingCafeOwnerClaims = emptyList(),
                            metrics = buildAdminMetrics(0),
                            infoMessage = ownerClaimResult.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun handlePendingResult(id: String, approved: Boolean) {
        val state = _uiState.value
        val selectedFilter = state.selectedPendingFilter
        val requestTitle = when (selectedFilter) {
            PendingFilter.CAFE_REGISTRATION -> {
                state.pendingCafeRegistrationClaims.firstOrNull { it.claimId == id }?.cafeName
            }
            PendingFilter.ROLE_CLAIM -> {
                state.pendingCafeOwnerClaims.firstOrNull { it.claimId == id }?.let { claim ->
                    "점장 권한 신청 - ${claim.requesterNickname}"
                }
            }
        } ?: return

        viewModelScope.launch {
            val result = when (selectedFilter) {
                PendingFilter.CAFE_REGISTRATION -> {
                    if (approved) {
                        approveCafeRegistrationClaimUseCase.invoke(id)
                    } else {
                        rejectCafeRegistrationClaimUseCase.invoke(id)
                    }
                }
                PendingFilter.ROLE_CLAIM -> {
                    if (approved) {
                        approveCafeOwnerClaimUseCase.invoke(id)
                    } else {
                        rejectCafeOwnerClaimUseCase.invoke(id)
                    }
                }
            }
            when (result) {
                is AppResult.Success -> {
                    loadPendingRequests()
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (approved) {
                                "$requestTitle 요청을 승인했습니다."
                            } else {
                                "$requestTitle 요청을 반려했습니다."
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = result.error.toString()) }
                }
            }
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
            AdminOperationsAction.ClickBannerRegister -> {
                viewModelScope.launch {
                    _event.emit(AdminOperationsEvent.NavigateToBannerEdit)
                }
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
                if (action.id == ADMIN_BANNER_MENU_ID) {
                    viewModelScope.launch {
                        _event.emit(AdminOperationsEvent.NavigateToBannerEdit)
                    }
                } else {
                    val label = _uiState.value.quickMenus.firstOrNull { it.id == action.id }?.title ?: "메뉴"
                    _uiState.update { it.copy(infoMessage = "$label 연결은 다음 단계에서 이어집니다.") }
                }
            }
            AdminOperationsAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    init {
        loadPendingRequests()
    }
}

private const val ADMIN_BANNER_MENU_ID = "banner"
