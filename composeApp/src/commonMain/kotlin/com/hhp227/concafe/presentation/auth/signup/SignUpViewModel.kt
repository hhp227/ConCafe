package com.hhp227.concafe.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.core.util.normalizeKoreanPhoneToE164
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.usecase.CreateCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.GetSignUpCafeListUseCase
import com.hhp227.concafe.domain.usecase.SignInWithKakaoIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignInWithGoogleIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserProfileUseCase
import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.KakaoIdTokenProvider

class SignUpViewModel(
    private val getSignUpCafeListUseCase: GetSignUpCafeListUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase,
    private val phoneAuthProvider: PhoneAuthProvider,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    private val signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val kakaoIdTokenProvider: KakaoIdTokenProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState.empty())
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
                hasRequestedVerification = false,
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
                hasRequestedVerification = false,
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
                hasRequestedVerification = false,
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

    private fun selectCafe(cafe: Cafe) {
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

    private fun clearCafeSelection() {
        _uiState.update {
            it.copy(
                selectedCafe = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun loadCafeOptions() {
        viewModelScope.launch {
            when (val result = getSignUpCafeListUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(cafes = result.data) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            cafes = emptyList(),
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun sendVerification() {
        val phone = uiState.value.phone.trim()
        val normalizedPhone = normalizeKoreanPhoneToE164(phone)

        if (normalizedPhone == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "휴대폰 번호 형식을 확인해주세요. 예: 010-1234-5678",
                    infoMessage = null
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = phoneAuthProvider.sendCode(normalizedPhone)

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasRequestedVerification = true,
                            errorMessage = null,
                            infoMessage = "인증번호가 전송되었습니다."
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasRequestedVerification = false,
                            errorMessage = resolvePhoneVerificationRequestErrorMessage(result.error),
                            infoMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun verifyCode() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (phoneAuthProvider.verifyCode(uiState.value.verificationCode)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPhoneVerified = true,
                            hasRequestedVerification = true,
                            errorMessage = null,
                            infoMessage = "휴대폰 인증이 완료되었습니다."
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPhoneVerified = false,
                            errorMessage = "인증번호가 일치하지 않습니다.",
                            infoMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun submit() {
        val currentState = uiState.value
        val validationMessage = validate(currentState)
        if (validationMessage != null) {
            _uiState.update { it.copy(errorMessage = validationMessage, infoMessage = null) }
            return
        }

        val nickname = resolveNickname(currentState)
        val role = resolveRole(currentState)
        val selectedCafeId = currentState.selectedCafe?.id
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            when (
                val result = signUpUseCase.invoke(
                    email = currentState.email.trim(),
                    password = currentState.password,
                    nickname = nickname,
                    role = role,
                    affiliatedCafeId = if (role == UserRole.CAST) selectedCafeId else null
                )
            ) {
                is AppResult.Success -> {
                    createOwnerCafeClaimIfNeeded(role, selectedCafeId)
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(SignUpEvent.SignedUp)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resolveSignUpErrorMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    private fun resolvePhoneVerificationRequestErrorMessage(error: AppError): String {
        val reason = when (error) {
            is AppError.ValidationFailed -> error.reason.uppercase()
            is AppError.Unknown -> (error.cause ?: "").uppercase()
            else -> ""
        }
        return if (reason.contains("INVALID_PHONE_NUMBER")) {
            "휴대폰 번호 형식을 확인해주세요. 예: 010-1234-5678"
        } else if (reason.contains("QUOTA_EXCEEDED")
            || reason.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else if (reason.contains("CAPTCHA_CHECK_FAILED")) {
            "인증 검증에 실패했습니다. 잠시 후 다시 시도해주세요."
        } else if (reason.contains("MISSING_APP_TOKEN")) {
            "앱 인증 설정이 필요합니다. 앱을 재실행 후 다시 시도해주세요."
        } else if (reason.contains("APP_NOT_VERIFIED")) {
            "앱 인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."
        } else {
            "인증번호 요청에 실패했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
        }
    }

    private suspend fun createOwnerCafeClaimIfNeeded(role: UserRole, selectedCafeId: String?) {
        if (role == UserRole.CAFE_OWNER && !selectedCafeId.isNullOrBlank()) {
            createCafeOwnerClaimUseCase.invoke(selectedCafeId)
        }
    }

    private fun socialSignUp(provider: SignUpProvider) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (provider) {
                SignUpProvider.GOOGLE -> {
                    runCatching { googleIdTokenProvider.getGoogleIdToken() }
                        .onFailure {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    errorMessage = "구글 회원가입에 실패했습니다. 다시 시도해주세요.",
                                    infoMessage = null
                                )
                            }
                        }
                        .onSuccess { idToken ->
                            when (signInWithGoogleIdTokenUseCase.invoke(idToken)) {
                                is AppResult.Success -> {
                                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                    _event.emit(SignUpEvent.SignedUp)
                                }
                                is AppResult.Failure -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = "구글 회원가입에 실패했습니다. 다시 시도해주세요.",
                                            infoMessage = null
                                        )
                                    }
                                }
                            }
                        }
                }
                SignUpProvider.APPLE -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "애플 회원가입은 iOS 앱에서 지원됩니다.",
                            infoMessage = null
                        )
                    }
                }
                SignUpProvider.KAKAO -> {
                    runCatching { kakaoIdTokenProvider.getKakaoAuthPayload() }
                        .onFailure {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    errorMessage = "카카오 회원가입에 실패했습니다. 다시 시도해주세요.",
                                    infoMessage = null
                                )
                            }
                        }
                        .onSuccess { payload ->
                            val email = payload.email?.trim()
                            val nickname = payload.nickname?.trim()

                            when (
                                signInWithKakaoIdTokenUseCase.invoke(
                                    idToken = payload.idToken,
                                    email = email,
                                    nickname = nickname
                                )
                            ) {
                                is AppResult.Success -> {
                                    val resolvedNickname = nickname.orEmpty()

                                    if (resolvedNickname.isNotBlank()) {
                                        updateUserProfileUseCase.invoke(
                                            nickname = resolvedNickname,
                                            profileImage = null
                                        )
                                    }
                                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                    _event.emit(SignUpEvent.SignedUp)
                                }
                                is AppResult.Failure -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = "카카오 회원가입에 실패했습니다. 다시 시도해주세요.",
                                            infoMessage = null
                                        )
                                    }
                                }
                            }
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
        if (userType == SignUpUiState.UserType.CAST && state.selectedCafe == null) return "카페를 선택해주세요."

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

    private fun resolveRole(state: SignUpUiState): UserRole {
        return when (state.selectedUserType) {
            SignUpUiState.UserType.CAFE_OWNER -> UserRole.CAFE_OWNER
            SignUpUiState.UserType.CAST -> UserRole.CAST
            SignUpUiState.UserType.VISITOR,
            null -> UserRole.VISITOR
        }
    }

    private fun resolveSignUpErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.ValidationFailed -> mapFirebaseSignUpReason(error.reason)
            is AppError.NetworkError -> error.message ?: "네트워크 오류로 회원가입에 실패했습니다."
            is AppError.Unknown -> mapFirebaseSignUpReason(error.cause ?: "")
            else -> "회원가입에 실패했습니다. 입력값을 확인해주세요."
        }
    }

    private fun mapFirebaseSignUpReason(reason: String): String {
        val normalizedReason = reason.uppercase()

        return if (
            normalizedReason.contains("EMAIL_EXISTS") ||
            normalizedReason.contains("EMAIL ALREADY EXISTS") ||
            normalizedReason.contains("EMAIL_ALREADY_IN_USE")
        ) {
            "이미 가입된 이메일입니다."
        } else if (
            normalizedReason.contains("INVALID_EMAIL")
        ) {
            "이메일 형식이 올바르지 않습니다."
        } else if (
            normalizedReason.contains("WEAK_PASSWORD") ||
            normalizedReason.contains("PASSWORD SHOULD BE AT LEAST")
        ) {
            "비밀번호 보안 강도가 낮습니다. 더 강한 비밀번호를 입력해주세요."
        } else if (
            normalizedReason.contains("TOO_MANY_ATTEMPTS_TRY_LATER")
        ) {
            "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else if (
            normalizedReason.contains("NETWORK")
        ) {
            "네트워크 오류로 회원가입에 실패했습니다."
        } else reason.ifBlank {
            "회원가입에 실패했습니다. 입력값을 확인해주세요."
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
            SignUpAction.ClickClearCafe -> clearCafeSelection()
            SignUpAction.ClickSubmit -> submit()
            is SignUpAction.ClickSocialSignUp -> socialSignUp(action.provider)
            SignUpAction.ClickSignInInstead -> viewModelScope.launch { _event.emit(SignUpEvent.NavigateBack) }
        }
    }

    init {
        loadCafeOptions()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
