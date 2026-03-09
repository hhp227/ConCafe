package org.hhp227.concafe.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.SignInUseCase
import org.hhp227.concafe.domain.usecase.SignUpUseCase

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SignUpUiState.empty().copy(cafes = defaultCafes)
    )
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<SignUpEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun selectUserType(type: SignUpUiState.UserType) {
        _uiState.update {
            it.copy(
                step = SignUpUiState.Step.FORM,
                selectedUserType = type,
                errorMessage = null,
                infoMessage = null,
                isCafeSearchVisible = false,
                cafeSearchQuery = "",
                selectedCafe = null,
                phone = "",
                verificationCode = "",
                isPhoneVerified = false
            )
        }
    }

    private fun handleBack() {
        if (uiState.value.step == SignUpUiState.Step.FORM) {
            backToTypeSelection()
        } else {
            viewModelScope.launch { _event.emit(SignUpEvent.NavigateBack) }
        }
    }

    private fun backToTypeSelection() {
        _uiState.update {
            it.copy(
                step = SignUpUiState.Step.SELECT_TYPE,
                selectedUserType = null,
                errorMessage = null,
                infoMessage = null,
                isCafeSearchVisible = false,
                cafeSearchQuery = "",
                selectedCafe = null,
                phone = "",
                verificationCode = "",
                isPhoneVerified = false
            )
        }
    }

    private fun changePhone(value: String) {
        _uiState.update {
            it.copy(
                phone = value,
                isPhoneVerified = false,
                verificationCode = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun changeCafeSearchQuery(value: String) {
        _uiState.update {
            it.copy(
                cafeSearchQuery = value,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun toggleCafeSearch() {
        _uiState.update {
            it.copy(
                isCafeSearchVisible = !it.isCafeSearchVisible,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun selectCafe(cafe: SignUpUiState.CafeOption) {
        _uiState.update {
            it.copy(
                selectedCafe = cafe,
                isCafeSearchVisible = false,
                cafeSearchQuery = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun sendVerification() {
        val phone = uiState.value.phone.trim()

        if (phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "휴대폰 번호를 입력해주세요.", infoMessage = null) }
            return
        }

        _uiState.update {
            it.copy(
                errorMessage = null,
                infoMessage = "인증번호가 $phone 로 전송되었습니다. 테스트 코드는 1234입니다."
            )
        }
    }

    private fun verifyCode() {
        if (uiState.value.verificationCode.trim() == VERIFICATION_CODE) {
            _uiState.update {
                it.copy(
                    isPhoneVerified = true,
                    errorMessage = null,
                    infoMessage = "휴대폰 인증이 완료되었습니다."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isPhoneVerified = false,
                    errorMessage = "인증번호가 일치하지 않습니다.",
                    infoMessage = null
                )
            }
        }
    }

    private fun submit() {
        val validationMessage = validate(uiState.value)
        if (validationMessage != null) {
            _uiState.update { it.copy(errorMessage = validationMessage, infoMessage = null) }
            return
        }

        val nickname = resolveNickname(uiState.value)
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            when (
                signUpUseCase.invoke(
                    email = uiState.value.email.trim(),
                    password = uiState.value.password,
                    nickname = nickname
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(SignUpEvent.SignedUp)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                        )
                    }
                }
            }
        }
    }

    private fun socialSignUp(provider: SignUpProvider) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            when (
                signInUseCase.invoke(
                    email = "${provider.name.lowercase()}@mock.concafe",
                    password = "social-sign-in"
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(SignUpEvent.SignedUp)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "소셜 회원가입에 실패했습니다. 잠시 후 다시 시도해주세요."
                        )
                    }
                }
            }
        }
    }

    private fun updateState(
        email: String = uiState.value.email,
        password: String = uiState.value.password,
        confirmPassword: String = uiState.value.confirmPassword,
        nickname: String = uiState.value.nickname,
        name: String = uiState.value.name,
        phone: String = uiState.value.phone,
        verificationCode: String = uiState.value.verificationCode
    ) {
        _uiState.update {
            it.copy(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                nickname = nickname,
                name = name,
                phone = phone,
                verificationCode = verificationCode,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun validate(state: SignUpUiState): String? {
        val userType = state.selectedUserType ?: return "회원 유형을 선택해주세요."

        if (state.email.isBlank()) return "이메일을 입력해주세요."
        if (userType == SignUpUiState.UserType.CAFE_OWNER && state.name.isBlank()) return "이름을 입력해주세요."
        if (userType != SignUpUiState.UserType.CAFE_OWNER && state.nickname.isBlank()) return "닉네임을 입력해주세요."
        if (state.password.length < MIN_PASSWORD_LENGTH) return "비밀번호는 8자 이상 입력해주세요."
        if (state.password != state.confirmPassword) return "비밀번호가 일치하지 않습니다."
        if (userType == SignUpUiState.UserType.CAFE_OWNER && !state.isPhoneVerified) return "휴대폰 인증을 완료해주세요."
        if (userType != SignUpUiState.UserType.VISITOR && state.selectedCafe == null) return "카페를 선택해주세요."

        return null
    }

    private fun resolveNickname(state: SignUpUiState): String {
        return when (state.selectedUserType) {
            SignUpUiState.UserType.CAFE_OWNER -> state.name.trim()
            SignUpUiState.UserType.CAST -> state.nickname.trim()
            SignUpUiState.UserType.VISITOR,
            null -> state.nickname.trim()
        }
    }

    fun onAction(action: SignUpAction) {
        when (action) {
            SignUpAction.ClickBack -> handleBack()
            is SignUpAction.ClickUserType -> selectUserType(action.type)
            SignUpAction.ClickBackToTypeSelection -> backToTypeSelection()
            is SignUpAction.ChangeEmail -> updateState(email = action.value)
            is SignUpAction.ChangePassword -> updateState(password = action.value)
            is SignUpAction.ChangeConfirmPassword -> updateState(confirmPassword = action.value)
            is SignUpAction.ChangeNickname -> updateState(nickname = action.value)
            is SignUpAction.ChangeName -> updateState(name = action.value)
            is SignUpAction.ChangePhone -> changePhone(action.value)
            is SignUpAction.ChangeVerificationCode -> updateState(verificationCode = action.value)
            is SignUpAction.ChangeCafeSearchQuery -> changeCafeSearchQuery(action.value)
            SignUpAction.ClickSendVerification -> sendVerification()
            SignUpAction.ClickVerifyCode -> verifyCode()
            SignUpAction.ClickToggleCafeSearch -> toggleCafeSearch()
            is SignUpAction.ClickCafe -> selectCafe(action.cafe)
            SignUpAction.ClickSubmit -> submit()
            is SignUpAction.ClickSocialSignUp -> socialSignUp(action.provider)
            SignUpAction.ClickSignInInstead -> viewModelScope.launch { _event.emit(SignUpEvent.NavigateBack) }
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val VERIFICATION_CODE = "1234"

        private val defaultCafes = listOf(
            SignUpUiState.CafeOption("cafe-1", "메이드 하우스", "강남", true),
            SignUpUiState.CafeOption("cafe-2", "핑크 캐슬", "신촌", true),
            SignUpUiState.CafeOption("cafe-3", "리본 카페", "홍대", true),
            SignUpUiState.CafeOption("cafe-4", "스위트 메이드", "명동", true)
        )
    }
}
