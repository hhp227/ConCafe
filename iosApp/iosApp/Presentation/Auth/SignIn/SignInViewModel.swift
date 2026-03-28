//
//  SignInViewModel.swift
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
class SignInViewModel: ObservableObject {
    private let signInUseCase: SignInUseCase

    private let signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase

    private let signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase

    private let signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase

    private let signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase

    private let updateUserProfileUseCase: UpdateUserProfileUseCase
    
    @Published private(set) var uiState = SignInUiState.empty
    
    let event = PassthroughSubject<SignInEvent, Never>()
    
    private var signInTask: Task<Void, Never>?

    private var webAuthSession: ASWebAuthenticationSession?

    private let webAuthPresentationContextProvider = WebAuthPresentationContextProvider()
    
    private func signIn(email: String, password: String) {
        uiState.isLoading = true
        uiState.errorMessage = nil
        
        signInTask?.cancel()
        signInTask = Task {
            do {
                let result = try await signInUseCase.invoke(email: email, password: password)
                
                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedIn)
                } else if result is AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "로그인에 실패했습니다. 입력값을 확인해주세요."
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "로그인에 실패했습니다. 입력값을 확인해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }
    
    func onAction(_ action: SignInAction) {
        switch action {
        case .emailChanged(let value):
            uiState.email = value
            uiState.errorMessage = nil
        case .passwordChanged(let value):
            uiState.password = value
            uiState.errorMessage = nil
        case .signInTapped:
            signIn(email: uiState.email, password: uiState.password)
        case .socialSignInTapped(let provider):
            uiState.isLoading = true
            uiState.errorMessage = nil
            signInTask?.cancel()
            signInTask = Task {
                if provider == .google {
                    await handleGoogleSignIn()
                    return
                } else if provider == .kakao {
                    await handleKakaoSignIn()
                    return
                }

                do {
                    let result = try await signInWithSocialProviderUseCase.invoke(provider: provider.rawValue)
                    if result is AppResultSuccess<AnyObject> {
                        uiState.isLoading = false
                        event.send(.signedIn)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = "소셜 로그인에 실패했습니다. 입력값을 확인해주세요."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.errorMessage = error.localizedDescription
                }
            }
        case .appleIdTokenReceived(let idToken):
            uiState.isLoading = true
            uiState.errorMessage = nil
            signInTask?.cancel()
            signInTask = Task {
                do {
                    let result = try await signInWithAppleIdTokenUseCase.invoke(idToken: idToken)

                    if result is AppResultSuccess<AnyObject> {
                        uiState.isLoading = false
                        event.send(.signedIn)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = "애플 로그인에 실패했습니다. 다시 시도해주세요."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.errorMessage = "애플 로그인에 실패했습니다. 다시 시도해주세요."
                }
            }
        }
    }
    
    init(
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase(),
        signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase = KoinInitializerKt.resolveSignInWithGoogleIdTokenUseCase(),
        signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase = KoinInitializerKt.resolveSignInWithAppleIdTokenUseCase(),
        signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase = KoinInitializerKt.resolveSignInWithKakaoIdTokenUseCase(),
        updateUserProfileUseCase: UpdateUserProfileUseCase = KoinInitializerKt.resolveUpdateUserProfileUseCase()
    ) {
        self.signInUseCase = signInUseCase
        self.signInWithSocialProviderUseCase = SignInWithSocialProviderUseCase(signInUseCase: signInUseCase)
        self.signInWithGoogleIdTokenUseCase = signInWithGoogleIdTokenUseCase
        self.signInWithAppleIdTokenUseCase = signInWithAppleIdTokenUseCase
        self.signInWithKakaoIdTokenUseCase = signInWithKakaoIdTokenUseCase
        self.updateUserProfileUseCase = updateUserProfileUseCase
    }
    
    deinit {
        signInTask?.cancel()
        webAuthSession?.cancel()
    }

    private func handleGoogleSignIn() async {
        do {
            let idToken = try await requestGoogleIdToken()
            let result = try await signInWithGoogleIdTokenUseCase.invoke(idToken: idToken)

            if result is AppResultSuccess<AnyObject> {
                uiState.isLoading = false
                event.send(.signedIn)
            } else {
                uiState.isLoading = false
                uiState.errorMessage = "구글 로그인에 실패했습니다. 다시 시도해주세요."
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = "구글 로그인에 실패했습니다. 다시 시도해주세요."
        }
    }

    private func handleKakaoSignIn() async {
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
                event.send(.signedIn)
            } else {
                uiState.isLoading = false
                uiState.errorMessage = "카카오 로그인에 실패했습니다. 다시 시도해주세요."
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = "카카오 로그인에 실패했습니다. 다시 시도해주세요."
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
            throw SignInError.invalidAuthUrl
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
                    continuation.resume(throwing: SignInError.emptyCallbackUrl)
                    return
                }

                let callbackState = self?.extractQueryValue(url: callbackUrl, key: "state")
                if callbackState != state {
                    continuation.resume(throwing: SignInError.invalidCallbackState)
                    return
                }

                guard
                    let authCode = self?.extractQueryValue(url: callbackUrl, key: "code"),
                    !authCode.isEmpty
                else {
                    continuation.resume(throwing: SignInError.authCodeNotFound)
                    return
                }

                continuation.resume(returning: authCode)
            }

            session.presentationContextProvider = self.webAuthPresentationContextProvider
            session.prefersEphemeralWebBrowserSession = false
            self.webAuthSession = session
            if !session.start() {
                self.webAuthSession = nil
                continuation.resume(throwing: SignInError.failedToStartWebAuth)
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
                    continuation.resume(throwing: SignInError.idTokenNotFound)
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
            throw SignInError.googleServiceConfigMissing
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
            throw SignInError.invalidAuthUrl
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
                    throw SignInError.idTokenNotFound
                }
            } else {
                throw SignInError.googleTokenExchangeFailed
            }
        } else {
            throw SignInError.googleTokenExchangeFailed
        }
    }

    private func urlEncoded(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(.init(charactersIn: "-._~"))
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}

private struct KakaoProfile {
    let email: String?
    let nickname: String?
}

private enum SignInError: Error {
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
