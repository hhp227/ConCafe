//
//  SignUpViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
class SignUpViewModel: ObservableObject {
    private let getSignUpCafeListUseCase: GetSignUpCafeListUseCase

    private let signUpUseCase: SignUpUseCase

    private let completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase

    private let createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase

    private let signInUseCase: SignInUseCase

    private let signInWithGoogleUseCase: SignInWithGoogleUseCase

    private let signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase

    private let signInWithKakaoUseCase: SignInWithKakaoUseCase

    private let signOutUseCase: SignOutUseCase

    private let requestPhoneVerificationCodeUseCase: RequestPhoneVerificationCodeUseCase

    private let verifyPhoneVerificationCodeUseCase: VerifyPhoneVerificationCodeUseCase

    private let linkPhoneCredentialUseCase: LinkPhoneCredentialUseCase

    private let linkEmailCredentialUseCase: LinkEmailCredentialUseCase

    private let discardIncompleteSignUpUseCase: DiscardIncompleteSignUpUseCase

    private let signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase

    @Published private(set) var uiState = SignUpUiState.empty

    let event = PassthroughSubject<SignUpEvent, Never>()

    private var requestTask: Task<Void, Never>?

    private func selectUserType(_ type: SignUpUiState.UserType) {
        uiState.step = .form
        uiState.selectedUserType = type
        uiState.isCafeOwner = type == .cafeOwner
        uiState.phone = ""
        uiState.phoneVerificationId = nil
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.signupCompleted = false
        uiState.isSocialFlow = false
        uiState.socialProvider = nil
        uiState.hasAuthenticatedSocialAccount = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false

        clearMessages()
    }

    private func handleBack() {
        cleanupIncompleteAccount()
        if uiState.step == .form {
            backToTypeSelection()
        } else {
            event.send(.navigateBack)
        }
    }

    private func backToTypeSelection() {
        uiState.step = .selectType
        uiState.selectedUserType = nil
        uiState.isCafeOwner = false
        uiState.phone = ""
        uiState.phoneVerificationId = nil
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.signupCompleted = false
        uiState.isSocialFlow = false
        uiState.socialProvider = nil
        uiState.hasAuthenticatedSocialAccount = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false
        clearMessages()
    }

    private func changePhone(_ value: String) {
        uiState.phone = value
        uiState.phoneVerificationId = nil
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.signupCompleted = false
        clearMessages()
    }

    private func clearCafeSelection() {
        uiState.selectedCafe = nil
        clearMessages()
    }

    private func loadCafeOptions() {
        requestTask?.cancel()
        requestTask = Task {
            do {
                let result = try await getSignUpCafeListUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let cafes = success.data as? [Cafe] {
                    uiState.cafes = cafes
                } else if let failure = result as? AppResultFailure {
                    uiState.cafes = []
                    uiState.errorMessage = "\(failure.error)"
                }
            } catch {
                if Task.isCancelled { return }
                uiState.cafes = []
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func sendVerification() {
        let trimmedPhone = uiState.phone.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedPhone = normalizeKoreanPhoneToE164(trimmedPhone)
        let requestedPhone: String

        if let normalizedPhone {
            requestedPhone = normalizedPhone
        } else {
            uiState.errorMessage = "휴대폰 번호 형식을 확인해주세요. 예: 010-1234-5678"
            uiState.infoMessage = nil
            return
        }

        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            do {
                let result = try await requestPhoneVerificationCodeUseCase.invoke(phoneNumber: requestedPhone)

                if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.hasRequestedVerification = false
                    uiState.phoneVerificationId = nil
                    uiState.errorMessage = resolvePhoneVerificationRequestErrorMessage(failure.error)
                    uiState.infoMessage = nil
                } else {
                    uiState.phoneVerificationId = "requested"
                    uiState.isLoading = false
                    uiState.hasRequestedVerification = true
                    uiState.signupCompleted = false
                    uiState.infoMessage = "인증번호가 전송되었습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.hasRequestedVerification = false
                uiState.phoneVerificationId = nil
                uiState.errorMessage = "인증번호 요청에 실패했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
                uiState.infoMessage = nil
            }
        }
    }

    private func verifyCode() {
        if !uiState.hasRequestedVerification {
            uiState.errorMessage = "인증번호 요청을 먼저 해주세요."
            uiState.infoMessage = nil
            return
        }
        let verificationCode = uiState.verificationCode
            .compactMap { char -> String? in
                if let value = char.wholeNumberValue {
                    return String(value)
                } else {
                    return nil
                }
            }
            .joined()
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if verificationCode.isEmpty {
            uiState.errorMessage = "인증번호를 입력해주세요."
            uiState.infoMessage = nil
            return
        } else if verificationCode.count != 6 {
            uiState.errorMessage = "인증번호 6자리를 입력해주세요."
            uiState.infoMessage = nil
            return
        }
        let isSocialCafeOwnerFlow = uiState.isSocialFlow && uiState.selectedUserType == .cafeOwner
        uiState.isLoading = true
        clearMessages()
        requestTask?.cancel()
        requestTask = Task {
            do {
                let result: AnyObject
                if isSocialCafeOwnerFlow {
                    result = try await linkPhoneCredentialUseCase.invoke(code: verificationCode)
                } else {
                    result = try await verifyPhoneVerificationCodeUseCase.invoke(code: verificationCode)
                }

                if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.isPhoneVerified = false
                    if isSocialCafeOwnerFlow {
                        uiState.errorMessage = resolvePhoneLinkErrorMessage(failure.error)
                    } else {
                        uiState.errorMessage = resolvePhoneVerificationCodeErrorMessage(failure.error)
                    }
                    uiState.infoMessage = nil
                } else {
                    uiState.isLoading = false
                    uiState.hasRequestedVerification = true
                    uiState.isPhoneVerified = true
                    uiState.signupCompleted = false
                    uiState.errorMessage = nil
                    uiState.infoMessage = "휴대폰 인증이 완료되었습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.isPhoneVerified = false
                uiState.errorMessage = isSocialCafeOwnerFlow
                    ? String(localized: String.LocalizationValue("signup_phone_link_failed"), table: "Localizable")
                    : "인증번호 확인에 실패했습니다. 다시 시도해주세요."
                uiState.infoMessage = nil
            }
        }
    }

    private func errorReason(_ error: AppError) -> String {
        if let validation = error as? AppErrorValidationFailed {
            return validation.reason.uppercased()
        } else if let unknown = error as? AppErrorUnknown {
            return (unknown.cause ?? "").uppercased()
        } else {
            return ""
        }
    }

    private func resolvePhoneVerificationCodeErrorMessage(_ error: AppError) -> String {
        let reason = errorReason(error)

        if reason.contains("INVALID_VERIFICATION_CODE") {
            return "인증번호가 일치하지 않습니다."
        } else if reason.contains("SESSION_EXPIRED") || reason.contains("INVALID_VERIFICATION_ID") {
            return "인증 세션이 만료되었습니다. 인증번호를 다시 요청해주세요."
        } else if reason.contains("TOO_MANY_ATTEMPTS_TRY_LATER") {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else if reason.contains("NETWORK") {
            return "인증번호 확인에 실패했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
        } else {
            return "인증번호 확인에 실패했습니다. 다시 시도해주세요."
        }
    }

    private func submit() {
        if let message = validate(uiState) {
            uiState.errorMessage = message
            uiState.infoMessage = nil
            return
        }

        let currentState = uiState
        let role = resolveRole(currentState)
        let selectedCafeId = currentState.selectedCafe?.id
        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            do {
                let normalizedEmail = currentState.email.trimmingCharacters(in: .whitespacesAndNewlines)
                let nickname = resolveNickname(currentState)
                let normalizedPhone = normalizeKoreanPhoneToE164(currentState.phone)

                if currentState.isSocialFlow {
                    let completeResult = try await completeSignUpForCurrentUserUseCase.invoke(
                        email: normalizedEmail,
                        nickname: nickname,
                        role: role,
                        affiliatedCafeId: role == .cast ? selectedCafeId : nil,
                        phoneNumber: role == .cafeOwner ? normalizedPhone : nil
                    )

                    if completeResult is AppResultSuccess<AnyObject> {
                        await createOwnerCafeClaimIfNeeded(role: role, selectedCafeId: selectedCafeId)
                        uiState.isLoading = false
                        uiState.signupCompleted = true
                        event.send(.signedUp)
                    } else if let failure = completeResult as? AppResultFailure {
                        uiState.isLoading = false
                        uiState.errorMessage = resolveSignUpErrorMessage(failure.error)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                    }
                } else if role == .cafeOwner {
                    let linkResult = await linkOwnerEmailCredential(email: normalizedEmail, password: currentState.password)

                    if linkResult == false {
                        uiState.isLoading = false
                        return
                    }
                    let signInResult = try await signInUseCase.invoke(
                        email: normalizedEmail,
                        password: currentState.password
                    )

                    if signInResult is AppResultSuccess<AnyObject> {
                        let completeResult = try await completeSignUpForCurrentUserUseCase.invoke(
                            email: normalizedEmail,
                            nickname: nickname,
                            role: role,
                            affiliatedCafeId: nil,
                            phoneNumber: normalizedPhone
                        )

                        if completeResult is AppResultSuccess<AnyObject> {
                            await createOwnerCafeClaimIfNeeded(role: role, selectedCafeId: selectedCafeId)
                            uiState.isLoading = false
                            uiState.signupCompleted = true
                            event.send(.signedUp)
                        } else if let failure = completeResult as? AppResultFailure {
                            uiState.isLoading = false
                            uiState.errorMessage = resolveSignUpErrorMessage(failure.error)
                        } else {
                            uiState.isLoading = false
                            uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                        }
                    } else if let failure = signInResult as? AppResultFailure {
                        uiState.isLoading = false
                        uiState.errorMessage = resolveSignUpErrorMessage(failure.error)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                    }
                } else {
                    let result = try await signUpUseCase.invoke(
                        email: normalizedEmail,
                        password: currentState.password,
                        nickname: nickname,
                        role: role,
                        affiliatedCafeId: role == .cast ? selectedCafeId : nil
                    )

                    if result is AppResultSuccess<AnyObject> {
                        await createOwnerCafeClaimIfNeeded(role: role, selectedCafeId: selectedCafeId)
                        uiState.isLoading = false
                        uiState.signupCompleted = true
                        event.send(.signedUp)
                    } else if let failure = result as? AppResultFailure {
                        uiState.isLoading = false
                        uiState.errorMessage = resolveSignUpErrorMessage(failure.error)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                    }
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func linkOwnerEmailCredential(email: String, password: String) async -> Bool {
        do {
            let result = try await linkEmailCredentialUseCase.invoke(email: email, password: password)

            if let failure = result as? AppResultFailure {
                uiState.errorMessage = resolveEmailLinkErrorMessage(failure.error)
                uiState.infoMessage = nil
                return false
            }
            return true
        } catch {
            uiState.errorMessage = "이메일 연결에 실패했습니다. 다시 시도해주세요."
            uiState.infoMessage = nil
            return false
        }
    }

    private func resolveEmailLinkErrorMessage(_ error: AppError) -> String {
        let reason = errorReason(error)

        if reason.contains("EMAIL_ALREADY_IN_USE") || reason.contains("CREDENTIAL_ALREADY_IN_USE") {
            return "이미 사용 중인 이메일입니다."
        } else if reason.contains("PROVIDER_ALREADY_LINKED") {
            return "이미 이메일 로그인이 연결된 계정입니다."
        } else if reason.contains("NO_CURRENT_USER") {
            return "휴대폰 인증 세션이 없습니다. 인증을 다시 진행해주세요."
        } else if reason.contains("INVALID_EMAIL") {
            return "이메일 형식이 올바르지 않습니다."
        } else if reason.contains("WEAK_PASSWORD") {
            return "비밀번호 보안 강도가 낮습니다. 더 강한 비밀번호를 입력해주세요."
        } else {
            return "이메일 연결에 실패했습니다. 다시 시도해주세요."
        }
    }

    private func cleanupIncompleteAccount() {
        let needsCleanup = !uiState.signupCompleted && (uiState.isPhoneVerified || uiState.hasAuthenticatedSocialAccount)

        if needsCleanup {
            requestTask?.cancel()
            requestTask = Task {
                if uiState.isSocialFlow {
                    _ = try? await signOutUseCase.invoke()
                    uiState.isPhoneVerified = false
                    uiState.hasRequestedVerification = false
                    uiState.phoneVerificationId = nil
                    uiState.signupCompleted = false
                    uiState.isSocialFlow = false
                    uiState.socialProvider = nil
                    uiState.hasAuthenticatedSocialAccount = false
                    uiState.errorMessage = nil
                    uiState.infoMessage = nil
                } else {
                    _ = try? await discardIncompleteSignUpUseCase.invoke()
                    uiState.isPhoneVerified = false
                    uiState.hasRequestedVerification = false
                    uiState.phoneVerificationId = nil
                    uiState.signupCompleted = false
                    uiState.errorMessage = nil
                    uiState.infoMessage = nil
                }
            }
        } else {
        }
    }

    private func resolvePhoneLinkErrorMessage(_ error: AppError) -> String {
        let reason = errorReason(error)

        if reason.contains("INVALID_VERIFICATION_CODE") {
            return "인증번호가 일치하지 않습니다."
        } else if reason.contains("SESSION_EXPIRED") || reason.contains("INVALID_VERIFICATION_ID") {
            return "인증 세션이 만료되었습니다. 인증번호를 다시 요청해주세요."
        } else if reason.contains("CREDENTIAL_ALREADY_IN_USE") || reason.contains("PHONE_NUMBER_ALREADY_EXISTS") {
            return String(localized: String.LocalizationValue("signup_phone_link_already_in_use"), table: "Localizable")
        } else if reason.contains("PROVIDER_ALREADY_LINKED") {
            return String(localized: String.LocalizationValue("signup_phone_link_already_linked"), table: "Localizable")
        } else if reason.contains("NO_CURRENT_USER") {
            return String(localized: String.LocalizationValue("signup_phone_link_no_social_session"), table: "Localizable")
        } else {
            return String(localized: String.LocalizationValue("signup_phone_link_failed"), table: "Localizable")
        }
    }

    private func resolvePhoneVerificationRequestErrorMessage(_ error: AppError) -> String {
        let reason = errorReason(error)

        if reason.contains("INVALID_PHONE_NUMBER") {
            return "휴대폰 번호 형식을 확인해주세요. 예: 010-1234-5678"
        } else if reason.contains("INVALID_APP_CREDENTIAL") {
            return "앱 인증 토큰이 유효하지 않습니다. 푸시 인증서/APNs 설정을 확인해주세요."
        } else if reason.contains("QUOTA_EXCEEDED") || reason.contains("TOO_MANY_ATTEMPTS_TRY_LATER") {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else if reason.contains("CAPTCHA_CHECK_FAILED") {
            return "인증 검증에 실패했습니다. 잠시 후 다시 시도해주세요."
        } else if reason.contains("MISSING_APP_TOKEN") {
            return "앱 인증 설정이 필요합니다. 앱을 재실행 후 다시 시도해주세요."
        } else if reason.contains("APP_NOT_VERIFIED") {
            return "앱 인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."
        } else if reason.contains("NETWORK") {
            return "네트워크 오류로 인증번호 요청에 실패했습니다. 네트워크 상태를 확인해주세요."
        } else if reason.contains("WEB_CONTEXT_CANCELLED") {
            return "인증 웹 화면이 취소되었습니다. 다시 시도해주세요."
        } else if reason.contains("WEB_CONTEXT_ALREADY_PRESENTED") {
            return "인증 화면이 이미 열려 있습니다. 잠시 후 다시 시도해주세요."
        } else if !reason.isEmpty {
            return "인증번호 요청에 실패했습니다. (\(reason))"
        } else {
            return "인증번호 요청에 실패했습니다. 네트워크 상태를 확인 후 다시 시도해주세요."
        }
    }

    private func createOwnerCafeClaimIfNeeded(role: UserRole, selectedCafeId: String?) async {
        if role == .cafeOwner, let selectedCafeId, !selectedCafeId.isEmpty {
            _ = try? await createCafeOwnerClaimUseCase.invoke(cafeId: selectedCafeId)
        }
    }

    private func applySocialProfile(
        provider: SignUpProvider,
        email: String,
        nickname: String,
        autoCompleteVisitor: Bool
    ) {
        uiState.isLoading = false
        uiState.errorMessage = nil
        uiState.infoMessage = autoCompleteVisitor ? nil : "소셜 인증이 완료되었습니다. 필요한 정보만 입력하면 가입이 완료됩니다."
        uiState.email = email
        uiState.nickname = nickname
        uiState.isSocialFlow = true
        uiState.socialProvider = provider
        uiState.hasAuthenticatedSocialAccount = true

        if autoCompleteVisitor {
            submit()
        }
    }

    private func socialSignUp(_ provider: SignUpProvider) {
        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            let autoCompleteVisitor = uiState.selectedUserType == .visitor
            if provider == .google {
                await handleGoogleSignUp(autoCompleteVisitor: autoCompleteVisitor)
                return
            } else if provider == .kakao {
                await handleKakaoSignUp(autoCompleteVisitor: autoCompleteVisitor)
                return
            }

            do {
                let result = try await signInWithSocialProviderUseCase.invoke(provider: provider.rawValue)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    uiState.errorMessage = "애플 회원가입은 전용 버튼으로 다시 시도해주세요."
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "소셜 회원가입에 실패했습니다. 입력값을 확인해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func signUpWithAppleIdToken(_ idToken: String) {
        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            do {
                let result = try await signInWithAppleIdTokenUseCase.invoke(idToken: idToken)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let user = success.data as? Shared.User {
                    applySocialProfile(
                        provider: .apple,
                        email: user.email,
                        nickname: user.nickname,
                        autoCompleteVisitor: uiState.selectedUserType == .visitor
                    )
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "애플 회원가입에 실패했습니다. 다시 시도해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "애플 회원가입에 실패했습니다. 다시 시도해주세요."
            }
        }
    }

    private func handleGoogleSignUp(autoCompleteVisitor: Bool) async {
        do {
            let result = try await signInWithGoogleUseCase.invoke()

            if let success = result as? AppResultSuccess<AnyObject>,
               let user = success.data as? Shared.User {
                applySocialProfile(
                    provider: .google,
                    email: user.email,
                    nickname: user.nickname,
                    autoCompleteVisitor: autoCompleteVisitor
                )
            } else {
                uiState.isLoading = false
                uiState.errorMessage = "구글 회원가입에 실패했습니다. 다시 시도해주세요."
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = "구글 회원가입에 실패했습니다. 다시 시도해주세요."
        }
    }

    private func handleKakaoSignUp(autoCompleteVisitor: Bool) async {
        do {
            let result = try await signInWithKakaoUseCase.invoke()

            if let success = result as? AppResultSuccess<AnyObject>,
               let user = success.data as? Shared.User {
                applySocialProfile(
                    provider: .kakao,
                    email: user.email,
                    nickname: user.nickname,
                    autoCompleteVisitor: autoCompleteVisitor
                )
            } else {
                uiState.isLoading = false
                uiState.errorMessage = "카카오 회원가입에 실패했습니다. 다시 시도해주세요."
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = "카카오 회원가입에 실패했습니다. 다시 시도해주세요."
        }
    }

    private func updateState(
        email: String? = nil,
        password: String? = nil,
        confirmPassword: String? = nil,
        nickname: String? = nil,
        name: String? = nil,
        verificationCode: String? = nil
    ) {
        if let email {
            uiState.email = email
        }
        if let password {
            uiState.password = password
        }
        if let confirmPassword {
            uiState.confirmPassword = confirmPassword
        }
        if let nickname {
            uiState.nickname = nickname
        }
        if let name {
            uiState.name = name
        }
        if let verificationCode {
            uiState.verificationCode = verificationCode
        }
        clearMessages()
    }

    private func clearMessages() {
        uiState.errorMessage = nil
        uiState.infoMessage = nil
    }

    private func validate(_ state: SignUpUiState) -> String? {
        guard let userType = state.selectedUserType else {
            return "회원 유형을 선택해주세요."
        }
        if state.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "이메일을 입력해주세요."
        }
        if userType == .cafeOwner && state.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "이름을 입력해주세요."
        }
        if userType != .cafeOwner && state.nickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "닉네임을 입력해주세요."
        }
        if !state.isSocialFlow && state.password.count < Self.minimumPasswordLength {
            return "비밀번호는 8자 이상 입력해주세요."
        }
        if !state.isSocialFlow && state.password != state.confirmPassword {
            return "비밀번호가 일치하지 않습니다."
        }
        if userType == .cafeOwner && !state.isPhoneVerified {
            return "휴대폰 인증을 완료해주세요."
        }
        if userType == .cast && state.selectedCafe == nil {
            return "카페를 선택해주세요."
        }

        return nil
    }

    private func resolveNickname(_ state: SignUpUiState) -> String {
        switch state.selectedUserType {
        case .cafeOwner:
            return state.name.trimmingCharacters(in: .whitespacesAndNewlines)
        case .cast:
            return state.nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        case .visitor, .none:
            return state.nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    private func resolveRole(_ state: SignUpUiState) -> UserRole {
        switch state.selectedUserType {
        case .cafeOwner:
            return .cafeOwner
        case .cast:
            return .cast
        case .visitor, .none:
            return .visitor
        }
    }

    private func resolveSignUpErrorMessage(_ error: AppError) -> String {
        if let validation = error as? AppErrorValidationFailed {
            return mapFirebaseSignUpReason(validation.reason)
        } else if let network = error as? AppErrorNetworkError {
            return network.message ?? "네트워크 오류로 회원가입에 실패했습니다."
        } else if let unknown = error as? AppErrorUnknown {
            return mapFirebaseSignUpReason(unknown.cause ?? "")
        } else {
            return "회원가입에 실패했습니다. 입력값을 확인해주세요."
        }
    }

    private func mapFirebaseSignUpReason(_ reason: String) -> String {
        let normalized = reason.uppercased()

        if normalized.contains("EMAIL_EXISTS")
            || normalized.contains("EMAIL ALREADY EXISTS")
            || normalized.contains("EMAIL_ALREADY_IN_USE") {
            return "이미 가입된 이메일입니다."
        } else if normalized.contains("INVALID_EMAIL") {
            return "이메일 형식이 올바르지 않습니다."
        } else if normalized.contains("WEAK_PASSWORD")
                    || normalized.contains("PASSWORD SHOULD BE AT LEAST") {
            return "비밀번호 보안 강도가 낮습니다. 더 강한 비밀번호를 입력해주세요."
        } else if normalized.contains("TOO_MANY_ATTEMPTS_TRY_LATER") {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
        } else if normalized.contains("NETWORK") {
            return "네트워크 오류로 회원가입에 실패했습니다."
        } else if !reason.isEmpty {
            return reason
        } else {
            return "회원가입에 실패했습니다. 입력값을 확인해주세요."
        }
    }

    func onAction(_ action: SignUpAction) {
        switch action {
        case .backTapped:
            handleBack()
        case .userTypeTapped(let type):
            selectUserType(type)
        case .backToTypeSelectionTapped:
            backToTypeSelection()
        case .emailChanged(let value):
            updateState(email: value)
        case .passwordChanged(let value):
            updateState(password: value)
        case .confirmPasswordChanged(let value):
            updateState(confirmPassword: value)
        case .nicknameChanged(let value):
            updateState(nickname: value)
        case .nameChanged(let value):
            updateState(name: value)
        case .phoneChanged(let value):
            changePhone(value)
        case .verificationCodeChanged(let value):
            updateState(verificationCode: value)
        case .cafeSearchQueryChanged(let value):
            uiState.cafeSearchQuery = value
            clearMessages()
        case .sendVerificationTapped:
            sendVerification()
        case .verifyCodeTapped:
            verifyCode()
        case .toggleCafeSearchTapped:
            uiState.isCafeSearchVisible.toggle()
            clearMessages()
        case .cafeTapped(let cafe):
            uiState.selectedCafe = cafe
            uiState.isCafeSearchVisible = false
            uiState.cafeSearchQuery = ""
            clearMessages()
        case .clearCafeTapped:
            clearCafeSelection()
        case .submitTapped:
            submit()
        case .socialSignUpTapped(let provider):
            socialSignUp(provider)
        case .appleIdTokenReceived(let idToken):
            signUpWithAppleIdToken(idToken)
        case .signInInsteadTapped:
            cleanupIncompleteAccount()
            event.send(.navigateBack)
        case .cleanupIncompleteAccount:
            cleanupIncompleteAccount()
        }
    }

    init(
        getSignUpCafeListUseCase: GetSignUpCafeListUseCase = KoinInitializerKt.resolveGetSignUpCafeListUseCase(),
        signUpUseCase: SignUpUseCase = KoinInitializerKt.resolveSignUpUseCase(),
        completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase = KoinInitializerKt.resolveCompleteSignUpForCurrentUserUseCase(),
        createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase = KoinInitializerKt.resolveCreateCafeOwnerClaimUseCase(),
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase(),
        signInWithGoogleUseCase: SignInWithGoogleUseCase = KoinInitializerKt.resolveSignInWithGoogleUseCase(),
        signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase = KoinInitializerKt.resolveSignInWithAppleIdTokenUseCase(),
        signInWithKakaoUseCase: SignInWithKakaoUseCase = KoinInitializerKt.resolveSignInWithKakaoUseCase(),
        signOutUseCase: SignOutUseCase = KoinInitializerKt.resolveSignOutUseCase(),
        requestPhoneVerificationCodeUseCase: RequestPhoneVerificationCodeUseCase = KoinInitializerKt.resolveRequestPhoneVerificationCodeUseCase(),
        verifyPhoneVerificationCodeUseCase: VerifyPhoneVerificationCodeUseCase = KoinInitializerKt.resolveVerifyPhoneVerificationCodeUseCase(),
        linkPhoneCredentialUseCase: LinkPhoneCredentialUseCase = KoinInitializerKt.resolveLinkPhoneCredentialUseCase(),
        linkEmailCredentialUseCase: LinkEmailCredentialUseCase = KoinInitializerKt.resolveLinkEmailCredentialUseCase(),
        discardIncompleteSignUpUseCase: DiscardIncompleteSignUpUseCase = KoinInitializerKt.resolveDiscardIncompleteSignUpUseCase()
    ) {
        self.getSignUpCafeListUseCase = getSignUpCafeListUseCase
        self.signUpUseCase = signUpUseCase
        self.completeSignUpForCurrentUserUseCase = completeSignUpForCurrentUserUseCase
        self.createCafeOwnerClaimUseCase = createCafeOwnerClaimUseCase
        self.signInUseCase = signInUseCase
        self.signInWithGoogleUseCase = signInWithGoogleUseCase
        self.signInWithAppleIdTokenUseCase = signInWithAppleIdTokenUseCase
        self.signInWithKakaoUseCase = signInWithKakaoUseCase
        self.signOutUseCase = signOutUseCase
        self.requestPhoneVerificationCodeUseCase = requestPhoneVerificationCodeUseCase
        self.verifyPhoneVerificationCodeUseCase = verifyPhoneVerificationCodeUseCase
        self.linkPhoneCredentialUseCase = linkPhoneCredentialUseCase
        self.linkEmailCredentialUseCase = linkEmailCredentialUseCase
        self.discardIncompleteSignUpUseCase = discardIncompleteSignUpUseCase
        self.signInWithSocialProviderUseCase = SignInWithSocialProviderUseCase(signInUseCase: signInUseCase)
        loadCafeOptions()
    }

    deinit {
        requestTask?.cancel()
    }

    private static let minimumPasswordLength = 8

}
