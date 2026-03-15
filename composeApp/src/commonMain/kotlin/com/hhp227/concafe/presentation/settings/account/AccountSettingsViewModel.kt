package com.hhp227.concafe.presentation.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.di.resolveGetMyInfoUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class AccountSettingsViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase = resolveGetMyInfoUseCase(),
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase = resolveObserveCurrentUserUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountSettingsUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AccountSettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadAccountSettings()
            }
        }
    }

    private fun loadAccountSettings() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getMyInfoUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.value = result.data.toUiState()
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "계정 정보를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.NavigateBack)
        }
    }

    private fun clickSaveUserInfo() {
        val state = _uiState.value
        when {
            state.nickname.isBlank() -> emitMessage("닉네임을 입력해 주세요.")
            !state.email.contains("@") -> emitMessage("올바른 이메일 형식을 입력해 주세요.")
            else -> emitMessage("계정 기본 정보를 저장했어요. 현재 단계에서는 로컬 상태에 반영됩니다.")
        }
    }

    private fun clickOpenCastEdit() {
        val state = _uiState.value
        if (state.castId.isNullOrBlank()) {
            emitMessage("연결된 캐스트 프로필이 아직 없습니다.")
            return
        }
        viewModelScope.launch {
            _event.emit(
                AccountSettingsEvent.NavigateToCastEdit(
                    cafeId = state.castCafeId,
                    castId = state.castId
                )
            )
        }
    }

    private fun clickOpenChangePassword() {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.NavigateToChangePassword)
        }
    }

    private fun clickShowDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = true,
                deleteConfirmation = ""
            )
        }
    }

    private fun clickDismissDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                deleteConfirmation = ""
            )
        }
    }

    private fun clickDeleteAccount() {
        val state = _uiState.value
        if (state.deleteConfirmation != DELETE_CONFIRMATION_TEXT) {
            emitMessage("'$DELETE_CONFIRMATION_TEXT'를 정확히 입력해 주세요.")
            return
        }
        _uiState.update {
            it.copy(
                isDeleteRequested = true,
                isDeleteDialogVisible = false,
                deleteConfirmation = ""
            )
        }
        emitMessage("회원탈퇴 요청 단계를 진행했어요. 실제 서버 삭제 연동은 후속 단계에서 연결됩니다.")
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.ShowMessage(message))
        }
    }

    fun onAction(action: AccountSettingsAction) {
        when (action) {
            AccountSettingsAction.ClickBack -> clickBack()
            is AccountSettingsAction.ChangeNickname -> _uiState.update { it.copy(nickname = action.value) }
            is AccountSettingsAction.ChangeEmail -> _uiState.update { it.copy(email = action.value) }
            AccountSettingsAction.ClickSaveUserInfo -> clickSaveUserInfo()
            AccountSettingsAction.ClickOpenCastEdit -> clickOpenCastEdit()
            AccountSettingsAction.ClickOpenChangePassword -> clickOpenChangePassword()
            AccountSettingsAction.ClickShowDeleteDialog -> clickShowDeleteDialog()
            AccountSettingsAction.ClickDismissDeleteDialog -> clickDismissDeleteDialog()
            is AccountSettingsAction.ChangeDeleteConfirmation -> _uiState.update {
                it.copy(deleteConfirmation = action.value)
            }
            AccountSettingsAction.ClickDeleteAccount -> clickDeleteAccount()
        }
    }

    init {
        loadAccountSettings()
        observeSession()
    }

    private companion object {
        private const val DELETE_CONFIRMATION_TEXT = "탈퇴"
    }
}

private fun MyInfoFeed.toUiState(): AccountSettingsUiState {
    val currentUser = user
    val currentCast = castDetail?.cast
    return AccountSettingsUiState(
        isLoading = false,
        errorMessage = null,
        nickname = currentUser?.nickname.orEmpty(),
        email = currentUser?.email.orEmpty(),
        role = currentUser?.role,
        memberSince = currentUser?.createdAt.orEmpty(),
        roleSummary = when (currentUser?.role) {
            UserRole.CAST -> "캐스트 계정으로 팬과의 접점을 관리하고 있어요."
            UserRole.CAFE_OWNER -> "운영 카페와 함께 계정 권한을 관리하고 있어요."
            UserRole.ADMIN -> "운영 관리용 관리자 계정입니다."
            UserRole.VISITOR -> "팬 활동과 리뷰 기록을 관리하는 일반 계정입니다."
            null -> "로그인이 필요한 화면입니다."
        },
        linkedCafeName = castDetail?.cafe?.name,
        ownedCafeCount = ownedCafes.size,
        castId = currentCast?.id,
        castCafeId = currentCast?.cafeId,
        castName = currentCast?.name.orEmpty(),
        castConceptRole = currentCast?.conceptRole.orEmpty(),
        castDescription = currentCast?.desc.orEmpty(),
        isDeleteDialogVisible = false,
        deleteConfirmation = "",
        isDeleteRequested = false
    )
}
