//
//  SignInViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import AuthenticationServices
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
        let nonce = UUID().uuidString
        let state = UUID().uuidString
        let redirectUri = "\(callbackScheme):/oauthredirect"
        let authUrlString =
            "https://accounts.google.com/o/oauth2/v2/auth" +
            "?response_type=id_token" +
            "&client_id=\(urlEncoded(clientId))" +
            "&redirect_uri=\(urlEncoded(redirectUri))" +
            "&scope=\(urlEncoded("openid email profile"))" +
            "&nonce=\(urlEncoded(nonce))" +
            "&state=\(urlEncoded(state))" +
            "&prompt=select_account"

        guard let authUrl = URL(string: authUrlString) else {
            throw SignInError.invalidAuthUrl
        }

        return try await withCheckedThrowingContinuation { continuation in
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

                guard
                    let fragment = callbackUrl.fragment,
                    let idToken = self?.extractFragmentValue(fragment: fragment, key: "id_token"),
                    !idToken.isEmpty
                else {
                    continuation.resume(throwing: SignInError.idTokenNotFound)
                    return
                }

                continuation.resume(returning: idToken)
            }

            session.presentationContextProvider = self.webAuthPresentationContextProvider
            session.prefersEphemeralWebBrowserSession = false
            self.webAuthSession = session
            if !session.start() {
                self.webAuthSession = nil
                continuation.resume(throwing: SignInError.failedToStartWebAuth)
            }
        }
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
    case idTokenNotFound
}

private final class WebAuthPresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        return scene?.windows.first ?? ASPresentationAnchor()
    }
}
