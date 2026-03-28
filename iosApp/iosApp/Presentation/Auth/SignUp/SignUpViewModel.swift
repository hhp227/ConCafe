//
//  SignUpViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import AuthenticationServices
import CryptoKit
import Security
import UIKit
import KakaoSDKAuth
import KakaoSDKUser
import Shared

@MainActor
class SignUpViewModel: ObservableObject {
    private let getSignUpCafeListUseCase: GetSignUpCafeListUseCase

    private let signUpUseCase: SignUpUseCase

    private let createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase

    private let signInUseCase: SignInUseCase

    private let signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase

    private let signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase

    private let signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase

    private let updateUserProfileUseCase: UpdateUserProfileUseCase

    private let requestPhoneVerificationCodeUseCase: RequestPhoneVerificationCodeUseCase

    private let verifyPhoneVerificationCodeUseCase: VerifyPhoneVerificationCodeUseCase

    private let signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase

    @Published private(set) var uiState = SignUpUiState.empty

    let event = PassthroughSubject<SignUpEvent, Never>()

    private var requestTask: Task<Void, Never>?

    private var webAuthSession: ASWebAuthenticationSession?

    private let webAuthPresentationContextProvider = WebAuthPresentationContextProvider()

    private func selectUserType(_ type: SignUpUiState.UserType) {
        uiState.step = .form
        uiState.selectedUserType = type
        uiState.phone = ""
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false
        clearMessages()
    }

    private func handleBack() {
        if uiState.step == .form {
            backToTypeSelection()
        } else {
            event.send(.navigateBack)
        }
    }

    private func backToTypeSelection() {
        uiState.step = .selectType
        uiState.selectedUserType = nil
        uiState.phone = ""
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false
        clearMessages()
    }

    private func changePhone(_ value: String) {
        uiState.phone = value
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
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

        guard !trimmedPhone.isEmpty else {
            uiState.errorMessage = "휴대폰 번호를 입력해주세요."
            uiState.infoMessage = nil
            return
        }

        uiState.errorMessage = nil
        let result = requestPhoneVerificationCodeUseCase.invoke(phone: trimmedPhone)
        if let success = result as? AppResultSuccess<AnyObject>,
           let message = success.data as? String {
            uiState.hasRequestedVerification = true
            uiState.infoMessage = message
        } else {
            uiState.hasRequestedVerification = false
            uiState.errorMessage = "휴대폰 번호를 다시 확인해주세요."
            uiState.infoMessage = nil
        }
    }

    private func verifyCode() {
        let result = verifyPhoneVerificationCodeUseCase.invoke(code: uiState.verificationCode)
        if result is AppResultSuccess<AnyObject> {
            uiState.hasRequestedVerification = true
            uiState.isPhoneVerified = true
            uiState.errorMessage = nil
            uiState.infoMessage = "휴대폰 인증이 완료되었습니다."
        } else {
            uiState.isPhoneVerified = false
            uiState.errorMessage = "인증번호가 일치하지 않습니다."
            uiState.infoMessage = nil
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
                let result = try await signUpUseCase.invoke(
                    email: currentState.email.trimmingCharacters(in: .whitespacesAndNewlines),
                    password: currentState.password,
                    nickname: resolveNickname(currentState),
                    role: role,
                    affiliatedCafeId: role == .cast ? selectedCafeId : nil
                )

                if result is AppResultSuccess<AnyObject> {
                    await createOwnerCafeClaimIfNeeded(role: role, selectedCafeId: selectedCafeId)
                    uiState.isLoading = false
                    event.send(.signedUp)
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = resolveSignUpErrorMessage(failure.error)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func createOwnerCafeClaimIfNeeded(role: UserRole, selectedCafeId: String?) async {
        if role == .cafeOwner, let selectedCafeId, !selectedCafeId.isEmpty {
            _ = try? await createCafeOwnerClaimUseCase.invoke(cafeId: selectedCafeId)
        }
    }

    private func socialSignUp(_ provider: SignUpProvider) {
        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            if provider == .google {
                await handleGoogleSignUp()
                return
            } else if provider == .kakao {
                await handleKakaoSignUp()
                return
            }

            do {
                let result = try await signInWithSocialProviderUseCase.invoke(provider: provider.rawValue)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedUp)
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

                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedUp)
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

    private func handleGoogleSignUp() async {
        do {
            let idToken = try await requestGoogleIdToken()
            let result = try await signInWithGoogleIdTokenUseCase.invoke(idToken: idToken)

            if result is AppResultSuccess<AnyObject> {
                uiState.isLoading = false
                event.send(.signedUp)
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

    private func handleKakaoSignUp() async {
        do {
            let idToken = try await requestKakaoIdToken()
            let profile = await requestKakaoProfile()
            let result = try await signInWithKakaoIdTokenUseCase.invoke(
                idToken: idToken,
                email: profile.email,
                nickname: profile.nickname
            )

            if result is AppResultSuccess<AnyObject> {
                await applyKakaoNicknameIfNeeded(profile.nickname)
                uiState.isLoading = false
                event.send(.signedUp)
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

    private func requestGoogleIdToken() async throws -> String {
        let clientId = try requireGoogleServiceValue(key: "CLIENT_ID")
        let callbackScheme = try requireGoogleServiceValue(key: "REVERSED_CLIENT_ID")
        let state = UUID().uuidString
        let redirectUri = "\(callbackScheme):/oauthredirect"
        let codeVerifier = makeGoogleCodeVerifier()
        let codeChallenge = makeGoogleCodeChallenge(codeVerifier: codeVerifier)
        let authUrlString =
            "https://accounts.google.com/o/oauth2/v2/auth" +
            "?response_type=code" +
            "&client_id=\(urlEncoded(clientId))" +
            "&redirect_uri=\(urlEncoded(redirectUri))" +
            "&scope=\(urlEncoded("openid email profile"))" +
            "&state=\(urlEncoded(state))" +
            "&code_challenge=\(urlEncoded(codeChallenge))" +
            "&code_challenge_method=S256" +
            "&prompt=select_account"

        guard let authUrl = URL(string: authUrlString) else {
            throw SignUpError.invalidAuthUrl
        }
        let authCode = try await withCheckedThrowingContinuation { continuation in
            let session = ASWebAuthenticationSession(
                url: authUrl,
                callbackURLScheme: callbackScheme
            ) { [weak self] callbackUrl, error in
                self?.webAuthSession = nil

                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let callbackUrl else {
                    continuation.resume(throwing: SignUpError.emptyCallbackUrl)
                    return
                }

                let callbackState = self?.extractQueryValue(url: callbackUrl, key: "state")
                if callbackState != state {
                    continuation.resume(throwing: SignUpError.invalidCallbackState)
                    return
                }

                guard
                    let authCode = self?.extractQueryValue(url: callbackUrl, key: "code"),
                    !authCode.isEmpty
                else {
                    continuation.resume(throwing: SignUpError.authCodeNotFound)
                    return
                }

                continuation.resume(returning: authCode)
            }

            session.presentationContextProvider = self.webAuthPresentationContextProvider
            session.prefersEphemeralWebBrowserSession = false
            self.webAuthSession = session
            if !session.start() {
                self.webAuthSession = nil
                continuation.resume(throwing: SignUpError.failedToStartWebAuth)
            }
        }

        return try await exchangeGoogleAuthCodeForIdToken(
            clientId: clientId,
            authCode: authCode,
            codeVerifier: codeVerifier,
            redirectUri: redirectUri
        )
    }

    private func requestKakaoIdToken() async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
            let loginCompletion: (OAuthToken?, Error?) -> Void = { token, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let idToken = token?.idToken, !idToken.isEmpty else {
                    continuation.resume(throwing: SignUpError.idTokenNotFound)
                    return
                }
                continuation.resume(returning: idToken)
            }

            if UserApi.isKakaoTalkLoginAvailable() {
                UserApi.shared.loginWithKakaoTalk(completion: loginCompletion)
            } else {
                UserApi.shared.loginWithKakaoAccount(completion: loginCompletion)
            }
        }
    }

    private func applyKakaoNicknameIfNeeded(_ nickname: String?) async {
        if let nickname, !nickname.isEmpty {
            _ = try? await updateUserProfileUseCase.invoke(
                nickname: nickname,
                profileImage: nil
            )
        }
    }

    private func requestKakaoProfile() async -> KakaoProfile {
        return await withCheckedContinuation { continuation in
            UserApi.shared.me { user, error in
                if error != nil {
                    continuation.resume(returning: KakaoProfile(email: nil, nickname: nil))
                } else {
                    let nickname = user?.kakaoAccount?.profile?.nickname?.trimmingCharacters(in: .whitespacesAndNewlines)
                    let email = user?.kakaoAccount?.email?.trimmingCharacters(in: .whitespacesAndNewlines)
                    continuation.resume(
                        returning: KakaoProfile(
                            email: email?.isEmpty == true ? nil : email,
                            nickname: nickname?.isEmpty == true ? nil : nickname
                        )
                    )
                }
            }
        }
    }

    private func requireGoogleServiceValue(key: String) throws -> String {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let dictionary = NSDictionary(contentsOfFile: path) as? [String: Any],
            let value = dictionary[key] as? String,
            !value.isEmpty
        else {
            throw SignUpError.googleServiceConfigMissing
        }

        return value
    }

    private func extractFragmentValue(fragment: String, key: String) -> String? {
        let pairs = fragment.split(separator: "&")
        for pair in pairs {
            let components = pair.split(separator: "=", maxSplits: 1)
            guard components.count == 2 else { continue }
            if components[0] == Substring(key) {
                return String(components[1]).removingPercentEncoding
            }
        }
        return nil
    }

    private func extractQueryValue(url: URL, key: String) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return components?.queryItems?.first(where: { $0.name == key })?.value
    }

    private func makeGoogleCodeVerifier() -> String {
        var randomBytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        return base64UrlEncode(Data(randomBytes))
    }

    private func makeGoogleCodeChallenge(codeVerifier: String) -> String {
        let verifierData = Data(codeVerifier.utf8)
        let digest = SHA256.hash(data: verifierData)
        return base64UrlEncode(Data(digest))
    }

    private func base64UrlEncode(_ data: Data) -> String {
        return data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    private func exchangeGoogleAuthCodeForIdToken(
        clientId: String,
        authCode: String,
        codeVerifier: String,
        redirectUri: String
    ) async throws -> String {
        guard let url = URL(string: "https://oauth2.googleapis.com/token") else {
            throw SignUpError.invalidAuthUrl
        }

        let body =
            "code=\(urlEncoded(authCode))" +
            "&client_id=\(urlEncoded(clientId))" +
            "&code_verifier=\(urlEncoded(codeVerifier))" +
            "&redirect_uri=\(urlEncoded(redirectUri))" +
            "&grant_type=authorization_code"

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = body.data(using: .utf8)

        let (data, response) = try await URLSession.shared.data(for: request)
        if let httpResponse = response as? HTTPURLResponse {
            if (200..<300).contains(httpResponse.statusCode) {
                let tokenResponse = try JSONDecoder().decode(GoogleTokenResponse.self, from: data)
                if let idToken = tokenResponse.idToken, !idToken.isEmpty {
                    return idToken
                } else {
                    throw SignUpError.idTokenNotFound
                }
            } else {
                throw SignUpError.googleTokenExchangeFailed
            }
        } else {
            throw SignUpError.googleTokenExchangeFailed
        }
    }

    private func urlEncoded(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(.init(charactersIn: "-._~"))
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
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
        if state.password.count < Self.minimumPasswordLength {
            return "비밀번호는 8자 이상 입력해주세요."
        }
        if state.password != state.confirmPassword {
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
            event.send(.navigateBack)
        }
    }

    init(
        getSignUpCafeListUseCase: GetSignUpCafeListUseCase = KoinInitializerKt.resolveGetSignUpCafeListUseCase(),
        signUpUseCase: SignUpUseCase = KoinInitializerKt.resolveSignUpUseCase(),
        createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase = KoinInitializerKt.resolveCreateCafeOwnerClaimUseCase(),
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase(),
        signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase = KoinInitializerKt.resolveSignInWithGoogleIdTokenUseCase(),
        signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase = KoinInitializerKt.resolveSignInWithAppleIdTokenUseCase(),
        signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase = KoinInitializerKt.resolveSignInWithKakaoIdTokenUseCase(),
        updateUserProfileUseCase: UpdateUserProfileUseCase = KoinInitializerKt.resolveUpdateUserProfileUseCase()
    ) {
        self.getSignUpCafeListUseCase = getSignUpCafeListUseCase
        self.signUpUseCase = signUpUseCase
        self.createCafeOwnerClaimUseCase = createCafeOwnerClaimUseCase
        self.signInUseCase = signInUseCase
        self.signInWithGoogleIdTokenUseCase = signInWithGoogleIdTokenUseCase
        self.signInWithAppleIdTokenUseCase = signInWithAppleIdTokenUseCase
        self.signInWithKakaoIdTokenUseCase = signInWithKakaoIdTokenUseCase
        self.updateUserProfileUseCase = updateUserProfileUseCase
        self.requestPhoneVerificationCodeUseCase = RequestPhoneVerificationCodeUseCase()
        self.verifyPhoneVerificationCodeUseCase = VerifyPhoneVerificationCodeUseCase()
        self.signInWithSocialProviderUseCase = SignInWithSocialProviderUseCase(signInUseCase: signInUseCase)
        loadCafeOptions()
    }

    deinit {
        requestTask?.cancel()
        webAuthSession?.cancel()
    }

    private static let minimumPasswordLength = 8

}

private struct KakaoProfile {
    let email: String?
    let nickname: String?
}

private enum SignUpError: Error {
    case googleServiceConfigMissing
    case invalidAuthUrl
    case failedToStartWebAuth
    case emptyCallbackUrl
    case invalidCallbackState
    case authCodeNotFound
    case idTokenNotFound
    case googleTokenExchangeFailed
}

private struct GoogleTokenResponse: Decodable {
    let idToken: String?

    private enum CodingKeys: String, CodingKey {
        case idToken = "id_token"
    }
}

private final class WebAuthPresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        return scene?.windows.first ?? ASPresentationAnchor()
    }
}
