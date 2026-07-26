//
//  AccountSettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
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
import KMPNativeCoroutinesAsync

@MainActor
final class AccountSettingsViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let deleteAccountUseCase: DeleteAccountUseCase

    private let updateUserProfileUseCase: UpdateUserProfileUseCase

    private var webAuthSession: ASWebAuthenticationSession?

    private let webAuthPresentationContextProvider = WebAuthPresentationContextProvider()

    @Published private(set) var uiState = AccountSettingsUiState.empty

    let event = PassthroughSubject<AccountSettingsEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.observeSession]?.cancel()
        tasks[.observeSession] = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.loadAccountSettings()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadAccountSettings() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.loadAccountSettings]?.cancel()
        tasks[.loadAccountSettings] = Task {
            do {
                let result = try await getMyInfoUseCase.invoke()
                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.MyInfoFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.myInfoFeed = feed
                    uiState.nicknameInput = feed.user?.nickname ?? ""
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = String(localized: String.LocalizationValue("account_settings_error_load_failed"), table: "Localizable")
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = String(localized: String.LocalizationValue("account_settings_error_load_failed"), table: "Localizable")
            }
        }
    }

    private func emitMessage(_ message: String) {
        event.send(.showMessage(message))
    }

    private func saveUserInfo() {
        let nicknameInput = uiState.nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
        let profileImage = uiState.myInfoFeed?.user?.profileImage

        if nicknameInput.isEmpty {
            emitMessage(String(localized: String.LocalizationValue("account_settings_message_enter_nickname"), table: "Localizable"))
        } else {
            uiState.isLoading = true
            uiState.errorMessage = nil

            tasks[.saveUserInfo]?.cancel()
            tasks[.saveUserInfo] = Task {
                do {
                    let result = try await updateUserProfileUseCase.invoke(nickname: nicknameInput, profileImage: profileImage)

                    if result is AppResultSuccess<AnyObject> {
                        uiState.isLoading = false
                        emitMessage(String(localized: String.LocalizationValue("account_settings_message_saved"), table: "Localizable"))
                        loadAccountSettings()
                    } else if let failure = result as? AppResultFailure {
                        let message = mapProfileUpdateFailureMessage(failure)
                        uiState.isLoading = false
                        uiState.errorMessage = nil
                        emitMessage(message)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = nil
                        emitMessage(String(localized: String.LocalizationValue("account_settings_error_profile_save_failed"), table: "Localizable"))
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    emitMessage(String(localized: String.LocalizationValue("account_settings_error_profile_save_failed"), table: "Localizable"))
                }
            }
        }
    }

    private func openCastEdit() {
        guard let cast = uiState.myInfoFeed?.castDetail?.cast, !cast.id.isEmpty else {
            emitMessage(String(localized: String.LocalizationValue("account_settings_message_cast_profile_missing"), table: "Localizable"))
            return
        }
        event.send(.navigateToCastEdit(cafeId: cast.cafeId, castId: cast.id))
    }

    private func openChangePassword() {
        event.send(.navigateToChangePassword)
    }

    private func showDeleteDialog() {
        uiState.isDeleteDialogVisible = true
        uiState.deletePassword = ""
        uiState.deletePasswordErrorMessage = nil
    }

    private func dismissDeleteDialog() {
        uiState.isDeleteDialogVisible = false
        uiState.deletePassword = ""
        uiState.deletePasswordErrorMessage = nil
    }

    private func deleteAccount() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.deleteAccount]?.cancel()
        tasks[.deleteAccount] = Task {
            do {
                let result: AnyObject
                switch uiState.authProvider {
                case .email, .unknown:
                    let password = uiState.deletePassword.trimmingCharacters(in: .whitespacesAndNewlines)
                    if password.isEmpty {
                        uiState.isLoading = false
                        uiState.deletePasswordErrorMessage = String(localized: String.LocalizationValue("account_settings_delete_password_required"), table: "Localizable")
                        return
                    }
                    result = try await deleteAccountUseCase.invoke(
                        request: DeleteAccountRequest(
                            provider: .email,
                            password: password,
                            idToken: nil
                        )
                    )
                case .google:
                    let idToken = try await requestGoogleIdToken()
                    result = try await deleteAccountUseCase.invoke(
                        request: DeleteAccountRequest(
                            provider: .google,
                            password: nil,
                            idToken: idToken
                        )
                    )
                case .kakao:
                    let idToken = try await requestKakaoIdToken()
                    result = try await deleteAccountUseCase.invoke(
                        request: DeleteAccountRequest(
                            provider: .kakao,
                            password: nil,
                            idToken: idToken
                        )
                    )
                case .apple:
                    uiState.isLoading = false
                    uiState.deletePasswordErrorMessage = String(localized: String.LocalizationValue("account_settings_error_apple_reauth_required"), table: "Localizable")
                    return
                default:
                    let password = uiState.deletePassword.trimmingCharacters(in: .whitespacesAndNewlines)
                    result = try await deleteAccountUseCase.invoke(
                        request: DeleteAccountRequest(
                            provider: .email,
                            password: password,
                            idToken: nil
                        )
                    )
                }
                if let failure = result as? AppResultFailure {
                    let message = mapDeleteFailureMessage(failure)
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.deletePasswordErrorMessage = message
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.isDeleteRequested = true
                    uiState.isDeleteDialogVisible = false
                    uiState.deletePassword = ""
                    uiState.deletePasswordErrorMessage = nil
                    event.send(.navigateToMain)
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = String(localized: String.LocalizationValue("account_settings_error_delete_failed"), table: "Localizable")
                emitMessage(String(localized: String.LocalizationValue("account_settings_error_delete_failed"), table: "Localizable"))
            }
        }
    }

    private func deleteAccountWithAppleIdToken(_ idToken: String) {
        uiState.isLoading = true
        uiState.errorMessage = nil
        uiState.deletePasswordErrorMessage = nil

        tasks[.deleteAccount]?.cancel()
        tasks[.deleteAccount] = Task {
            do {
                let result = try await deleteAccountUseCase.invoke(
                    request: DeleteAccountRequest(
                        provider: .apple,
                        password: nil,
                        idToken: idToken
                    )
                )

                if let failure = result as? AppResultFailure {
                    let message = mapDeleteFailureMessage(failure)
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.deletePasswordErrorMessage = message
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.isDeleteRequested = true
                    uiState.isDeleteDialogVisible = false
                    uiState.deletePassword = ""
                    uiState.deletePasswordErrorMessage = nil
                    event.send(.navigateToMain)
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = String(localized: String.LocalizationValue("account_settings_error_delete_failed"), table: "Localizable")
                emitMessage(String(localized: String.LocalizationValue("account_settings_error_delete_failed"), table: "Localizable"))
            }
        }
    }

    private func mapDeleteFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("INVALID PASSWORD")
            || rawError.contains("INVALID_LOGIN_CREDENTIALS")
            || rawError.contains("INVALID_PASSWORD")
            || rawError.contains("EMAIL_NOT_FOUND") {
            return String(localized: String.LocalizationValue("account_settings_error_delete_invalid_password"), table: "Localizable")
        } else if rawError.contains("SOCIAL IDTOKEN IS REQUIRED") {
            return String(localized: String.LocalizationValue("account_settings_error_apple_reauth_required"), table: "Localizable")
        } else {
            return String(localized: String.LocalizationValue("account_settings_error_delete_failed"), table: "Localizable")
        }
    }

    private func mapProfileUpdateFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("UNAUTHORIZED") {
            return String(localized: String.LocalizationValue("account_settings_error_session_expired"), table: "Localizable")
        } else if rawError.contains("VALIDATIONFAILED") {
            return String(localized: String.LocalizationValue("account_settings_message_enter_nickname"), table: "Localizable")
        } else {
            return String(localized: String.LocalizationValue("account_settings_error_profile_save_failed"), table: "Localizable")
        }
    }

    func onAction(_ action: AccountSettingsAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .nicknameChanged(let value):
            uiState.nicknameInput = value
        case .saveUserInfoTapped:
            saveUserInfo()
        case .openCastEditTapped:
            openCastEdit()
        case .openChangePasswordTapped:
            openChangePassword()
        case .showDeleteDialogTapped:
            showDeleteDialog()
        case .dismissDeleteDialogTapped:
            dismissDeleteDialog()
        case .deletePasswordChanged(let value):
            uiState.deletePassword = value
            uiState.deletePasswordErrorMessage = nil
        case .deleteAccountTapped:
            deleteAccount()
        case .appleDeleteIdTokenReceived(let idToken):
            deleteAccountWithAppleIdToken(idToken)
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        deleteAccountUseCase: DeleteAccountUseCase = KoinInitializerKt.resolveDeleteAccountUseCase(),
        updateUserProfileUseCase: UpdateUserProfileUseCase = KoinInitializerKt.resolveUpdateUserProfileUseCase()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.deleteAccountUseCase = deleteAccountUseCase
        self.updateUserProfileUseCase = updateUserProfileUseCase

        loadAccountSettings()
        observeSession()
    }

    deinit {
        tasks.values.forEach { task in
            task.cancel()
        }
        tasks.removeAll()
        webAuthSession?.cancel()
    }

    private enum TaskKey {
        case observeSession
        case loadAccountSettings
        case saveUserInfo
        case deleteAccount
    }
}

private extension AccountSettingsViewModel {
    func requireGoogleServiceValue(key: String) throws -> String {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let dictionary = NSDictionary(contentsOfFile: path) as? [String: Any],
            let value = dictionary[key] as? String,
            !value.isEmpty
        else {
            throw AccountSettingsDeleteError.googleConfigMissing
        }
        return value
    }

    func requestGoogleIdToken() async throws -> String {
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
            throw AccountSettingsDeleteError.invalidAuthUrl
        }

        let authCode: String = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<String, Error>) in
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
                    continuation.resume(throwing: AccountSettingsDeleteError.emptyCallbackUrl)
                    return
                }
                let callbackState = self?.extractQueryValue(url: callbackUrl, key: "state")
                if callbackState != state {
                    continuation.resume(throwing: AccountSettingsDeleteError.invalidCallbackState)
                    return
                }
                guard
                    let authCode = self?.extractQueryValue(url: callbackUrl, key: "code"),
                    !authCode.isEmpty
                else {
                    continuation.resume(throwing: AccountSettingsDeleteError.authCodeNotFound)
                    return
                }
                continuation.resume(returning: authCode)
            }
            session.presentationContextProvider = self.webAuthPresentationContextProvider
            session.prefersEphemeralWebBrowserSession = false
            self.webAuthSession = session
            if !session.start() {
                self.webAuthSession = nil
                continuation.resume(throwing: AccountSettingsDeleteError.failedToStartWebAuth)
            }
        }

        return try await exchangeGoogleAuthCodeForIdToken(
            clientId: clientId,
            authCode: authCode,
            codeVerifier: codeVerifier,
            redirectUri: redirectUri
        )
    }

    func requestKakaoIdToken() async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
            let loginCompletion: (OAuthToken?, Error?) -> Void = { token, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let idToken = token?.idToken, !idToken.isEmpty else {
                    continuation.resume(throwing: AccountSettingsDeleteError.idTokenNotFound)
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

    func extractQueryValue(url: URL, key: String) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return components?.queryItems?.first(where: { $0.name == key })?.value
    }

    func makeGoogleCodeVerifier() -> String {
        var randomBytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        return base64UrlEncode(Data(randomBytes))
    }

    func makeGoogleCodeChallenge(codeVerifier: String) -> String {
        let verifierData = Data(codeVerifier.utf8)
        let digest = SHA256.hash(data: verifierData)
        return base64UrlEncode(Data(digest))
    }

    func base64UrlEncode(_ data: Data) -> String {
        data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    func exchangeGoogleAuthCodeForIdToken(
        clientId: String,
        authCode: String,
        codeVerifier: String,
        redirectUri: String
    ) async throws -> String {
        guard let url = URL(string: "https://oauth2.googleapis.com/token") else {
            throw AccountSettingsDeleteError.invalidAuthUrl
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
        guard let httpResponse = response as? HTTPURLResponse, (200..<300).contains(httpResponse.statusCode) else {
            throw AccountSettingsDeleteError.googleTokenExchangeFailed
        }
        let tokenResponse = try JSONDecoder().decode(AccountDeleteGoogleTokenResponse.self, from: data)
        guard let idToken = tokenResponse.idToken, !idToken.isEmpty else {
            throw AccountSettingsDeleteError.idTokenNotFound
        }
        return idToken
    }

    func urlEncoded(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(.init(charactersIn: "-._~"))
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}

private struct AccountDeleteGoogleTokenResponse: Decodable {
    let idToken: String?

    private enum CodingKeys: String, CodingKey {
        case idToken = "id_token"
    }
}

private enum AccountSettingsDeleteError: Error {
    case googleConfigMissing
    case invalidAuthUrl
    case emptyCallbackUrl
    case invalidCallbackState
    case authCodeNotFound
    case failedToStartWebAuth
    case idTokenNotFound
    case googleTokenExchangeFailed
}

private final class WebAuthPresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow } ?? UIWindow()
    }
}
