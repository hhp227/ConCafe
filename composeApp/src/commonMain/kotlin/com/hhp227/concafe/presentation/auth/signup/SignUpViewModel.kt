package com.hhp227.concafe.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.signup_phone_link_already_in_use
import concafe.composeapp.generated.resources.signup_phone_link_already_linked
import concafe.composeapp.generated.resources.signup_phone_link_failed
import concafe.composeapp.generated.resources.signup_phone_link_no_social_session
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
import com.hhp227.concafe.domain.usecase.CompleteSignUpForCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.GetSignUpCafeListUseCase
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignInWithKakaoIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignInWithGoogleIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserProfileUseCase
import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.KakaoIdTokenProvider
import org.jetbrains.compose.resources.getString

class SignUpViewModel(
    private val getSignUpCafeListUseCase: GetSignUpCafeListUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase,
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
                isCafeOwner = type == SignUpUiState.UserType.CAFE_OWNER,
                phone = "",
                phoneVerificationId = null,
                verificationCode = "",
                hasRequestedVerification = false,
                isPhoneVerified = false,
                signupCompleted = false,
                isSocialFlow = false,
                socialProvider = null,
                hasAuthenticatedSocialAccount = false
            )
        }
    }

    private fun handleBack() {
        cleanupIncompleteAccount()
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
                isCafeOwner = false,
                phone = "",
                phoneVerificationId = null,
                verificationCode = "",
                hasRequestedVerification = false,
                isPhoneVerified = false,
                signupCompleted = false,
                isSocialFlow = false,
                socialProvider = null,
                hasAuthenticatedSocialAccount = false
            )
        }
    }

    private fun changePhone(value: String) {
        _uiState.update {
            it.copy(
                phone = value,
                isPhoneVerified = false,
                phoneVerificationId = null,
                verificationCode = "",
                hasRequestedVerification = false,
                errorMessage = null,
                infoMessage = null,
                signupCompleted = false
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
                            phoneVerificationId = "requested",
                            signupCompleted = false,
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
                            phoneVerificationId = null,
                            errorMessage = resolvePhoneVerificationRequestErrorMessage(result.error),
                            infoMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun verifyCode() {
        val code = uiState.value.verificationCode
            .mapNotNull { char ->
                val digit = char.digitToIntOrNull()

                if (digit != null) {
                    digit.toString()
                } else {
                    null
                }
            }
            .joinToString(separator = "")

        if (code.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPhoneVerified = false,
                    phoneVerificationId = null,
                    errorMessage = "인증번호를 입력해주세요.",
                    infoMessage = null
                )
            }
            return
        } else if (code.length != 6) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPhoneVerified = false,
                    phoneVerificationId = null,
                    errorMessage = "인증번호 6자리를 입력해주세요.",
                    infoMessage = null
                )
            }
            return
        }

        val isSocialCafeOwnerFlow =
            uiState.value.isSocialFlow && uiState.value.selectedUserType == SignUpUiState.UserType.CAFE_OWNER

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = if (isSocialCafeOwnerFlow) phoneAuthProvider.linkPhone(code) else phoneAuthProvider.verifyCode(code)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPhoneVerified = true,
                            hasRequestedVerification = true,
                            signupCompleted = false,
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
                            errorMessage = if (isSocialCafeOwnerFlow) {
                                resolvePhoneLinkErrorMessage(result.error)
                            } else {
                                resolvePhoneVerificationCodeErrorMessage(result.error)
                            },
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
        val normalizedEmail = currentState.email.trim()
        val normalizedPhone = normalizeKoreanPhoneToE164(currentState.phone.trim())

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            if (currentState.isSocialFlow) {
                when (
                    val result = completeSignUpForCurrentUserUseCase.invoke(
                        email = normalizedEmail,
                        nickname = nickname,
                        role = role,
                        affiliatedCafeId = if (role == UserRole.CAST) selectedCafeId else null,
                        phoneNumber = if (role == UserRole.CAFE_OWNER) normalizedPhone else null
                    )
                ) {
                    is AppResult.Success -> {
                        createOwnerCafeClaimIfNeeded(role, selectedCafeId)
                        _uiState.update { it.copy(isLoading = false, signupCompleted = true) }
                        _event.emit(SignUpEvent.SignedUp)
                    }
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = resolveSignUpErrorMessage(result.error)) }
                    }
                }
            } else if (role == UserRole.CAFE_OWNER) {
                when (val linkResult = phoneAuthProvider.linkEmail(normalizedEmail, currentState.password)) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resolveOwnerEmailLinkErrorMessage(linkResult.error),
                                infoMessage = null
                            )
                        }
                        return@launch
                    }
                }
                when (val signInResult = signInUseCase.invoke(normalizedEmail, currentState.password)) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = resolveSignUpErrorMessage(signInResult.error)) }
                        return@launch
                    }
                }
                when (
                    val result = completeSignUpForCurrentUserUseCase.invoke(
                        email = normalizedEmail,
                        nickname = nickname,
                        role = role,
                        affiliatedCafeId = null,
                        phoneNumber = normalizedPhone
                    )
                ) {
                    is AppResult.Success -> {
                        createOwnerCafeClaimIfNeeded(role, selectedCafeId)
                        _uiState.update { it.copy(isLoading = false, signupCompleted = true) }
                        _event.emit(SignUpEvent.SignedUp)
                    }
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = resolveSignUpErrorMessage(result.error)) }
                    }
                }
            } else {
                when (
                    val result = signUpUseCase.invoke(
                        email = normalizedEmail,
                        password = currentState.password,
                        nickname = nickname,
                        role = role,
                        affiliatedCafeId = if (role == UserRole.CAST) selectedCafeId else null
                    )
                ) {
                    is AppResult.Success -> {
                        createOwnerCafeClaimIfNeeded(role, selectedCafeId)
                        _uiState.update { it.copy(isLoading = false, signupCompleted = true) }
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
    }

    private fun resolvePhoneVerificationRequestErrorMessage(error: AppError): String {
        val reason = when (error) {
            is AppError.ValidationFailed -> error.reason.uppercase()
            is AppError.Unknown -> (error.cause ?: "").uppercase()
            else -> ""
        }
        return if (reason.contains("INVALID_PHONE_NUMBER")) {
            "휴대폰 번호 형식을 확인해주세요. 예: 010-1234-5678"
        } else if (reason.contains("INVALID_APP_CREDENTIAL")) {
            "앱 인증 토큰이 유효하지 않습니다. Firebase/APNs 설정을 확인해주세요."
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
            if (reason.isNotBlank()) {
                "인증번호 요청에 실패했습니다. ($reason)"
            } else {
                "인증번호 요청에 실패했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
            }
        }
    }

    private fun resolvePhoneVerificationCodeErrorMessage(error: AppError): String {
        val reason = when (error) {
            is AppError.ValidationFailed -> error.reason.uppercase()
            is AppError.Unknown -> (error.cause ?: "").uppercase()
            else -> ""
        }

        return if (reason.contains("INVALID_VERIFICATION_CODE")) {
            "인증번호가 일치하지 않습니다."
        } else if (reason.contains("SESSION_EXPIRED")
            || reason.contains("INVALID_VERIFICATION_ID")) {
            "인증 세션이 만료되었습니다. 인증번호를 다시 요청해주세요."
        } else if (reason.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else {
            "인증번호 확인에 실패했습니다. 다시 시도해주세요."
        }
    }

    private fun resolveOwnerEmailLinkErrorMessage(error: AppError): String {
        val reason = when (error) {
            is AppError.ValidationFailed -> error.reason.uppercase()
            is AppError.Unknown -> (error.cause ?: "").uppercase()
            else -> ""
        }

        return if (reason.contains("EMAIL_ALREADY_IN_USE")
            || reason.contains("CREDENTIAL_ALREADY_IN_USE")) {
            "이미 사용 중인 이메일입니다."
        } else if (reason.contains("PROVIDER_ALREADY_LINKED")) {
            "이미 이메일 로그인이 연결된 계정입니다."
        } else if (reason.contains("NO_CURRENT_USER")) {
            "휴대폰 인증 세션이 없습니다. 인증을 다시 진행해주세요."
        } else if (reason.contains("INVALID_EMAIL")) {
            "이메일 형식이 올바르지 않습니다."
        } else if (reason.contains("WEAK_PASSWORD")) {
            "비밀번호 보안 강도가 낮습니다. 더 강한 비밀번호를 입력해주세요."
        } else {
            "이메일 연결에 실패했습니다. 다시 시도해주세요."
        }
    }

    private suspend fun resolvePhoneLinkErrorMessage(error: AppError): String {
        val reason = when (error) {
            is AppError.ValidationFailed -> error.reason.uppercase()
            is AppError.Unknown -> (error.cause ?: "").uppercase()
            else -> ""
        }

        return if (reason.contains("INVALID_VERIFICATION_CODE")) {
            "인증번호가 일치하지 않습니다."
        } else if (reason.contains("SESSION_EXPIRED")
            || reason.contains("INVALID_VERIFICATION_ID")) {
            "인증 세션이 만료되었습니다. 인증번호를 다시 요청해주세요."
        } else if (reason.contains("CREDENTIAL_ALREADY_IN_USE")
            || reason.contains("PHONE_NUMBER_ALREADY_EXISTS")) {
            getString(Res.string.signup_phone_link_already_in_use)
        } else if (reason.contains("PROVIDER_ALREADY_LINKED")) {
            getString(Res.string.signup_phone_link_already_linked)
        } else if (reason.contains("NO_CURRENT_USER")) {
            getString(Res.string.signup_phone_link_no_social_session)
        } else {
            getString(Res.string.signup_phone_link_failed)
        }
    }

    private fun cleanupIncompleteAccount() {
        val currentState = uiState.value
        val needsCleanup = currentState.isPhoneVerified && !currentState.signupCompleted

        if (needsCleanup) {
            viewModelScope.launch {
                if (currentState.isSocialFlow) {
                    _uiState.update {
                        it.copy(
                            isPhoneVerified = false,
                            hasRequestedVerification = false,
                            phoneVerificationId = null,
                            signupCompleted = false,
                            infoMessage = null,
                            errorMessage = null
                        )
                    }
                } else {
                    when (phoneAuthProvider.cleanupIncompleteAccount()) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isPhoneVerified = false,
                                    hasRequestedVerification = false,
                                    phoneVerificationId = null,
                                    signupCompleted = false,
                                    infoMessage = null,
                                    errorMessage = null
                                )
                            }
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isPhoneVerified = false,
                                    hasRequestedVerification = false,
                                    phoneVerificationId = null,
                                    signupCompleted = false,
                                    infoMessage = null,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Unit
        }
    }

    private suspend fun createOwnerCafeClaimIfNeeded(role: UserRole, selectedCafeId: String?) {
        if (role == UserRole.CAFE_OWNER && !selectedCafeId.isNullOrBlank()) {
            createCafeOwnerClaimUseCase.invoke(selectedCafeId)
        }
    }

    private fun applySocialProfile(
        provider: SignUpProvider,
        email: String,
        nickname: String,
        autoCompleteVisitor: Boolean
    ) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                infoMessage = if (autoCompleteVisitor) null else "소셜 인증이 완료되었습니다. 필요한 정보만 입력하면 가입이 완료됩니다.",
                email = email,
                nickname = nickname,
                isSocialFlow = true,
                socialProvider = provider,
                hasAuthenticatedSocialAccount = true
            )
        }
        if (autoCompleteVisitor) {
            submit()
        }
    }

    private fun socialSignUp(provider: SignUpProvider) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val autoCompleteVisitor = uiState.value.selectedUserType == SignUpUiState.UserType.VISITOR
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
                            when (val result = signInWithGoogleIdTokenUseCase.invoke(idToken)) {
                                is AppResult.Success -> {
                                    applySocialProfile(
                                        provider = provider,
                                        email = result.data.email,
                                        nickname = result.data.nickname,
                                        autoCompleteVisitor = autoCompleteVisitor
                                    )
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

                            when (val result =
                                signInWithKakaoIdTokenUseCase.invoke(
                                    idToken = payload.idToken,
                                    email = email,
                                    nickname = nickname
                                )
                            ) {
                                is AppResult.Success -> {
                                    val resolvedNickname = nickname.orEmpty().ifBlank { result.data.nickname }

                                    if (resolvedNickname.isNotBlank()) {
                                        updateUserProfileUseCase.invoke(
                                            nickname = resolvedNickname,
                                            profileImage = null
                                        )
                                    }
                                    applySocialProfile(
                                        provider = provider,
                                        email = email.orEmpty().ifBlank { result.data.email },
                                        nickname = resolvedNickname.ifBlank { result.data.nickname },
                                        autoCompleteVisitor = autoCompleteVisitor
                                    )
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
        if (!state.isSocialFlow && state.password.length < MIN_PASSWORD_LENGTH) return "비밀번호는 8자 이상 입력해주세요."
        if (!state.isSocialFlow && state.password != state.confirmPassword) return "비밀번호가 일치하지 않습니다."
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
            SignUpAction.ClickSignInInstead -> {
                cleanupIncompleteAccount()
                viewModelScope.launch { _event.emit(SignUpEvent.NavigateBack) }
            }
            SignUpAction.CleanupIncompleteAccount -> cleanupIncompleteAccount()
        }
    }

    init {
        loadCafeOptions()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
