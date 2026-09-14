//
//  SignInViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
class SignInViewModel: ObservableObject {
    private let signInUseCase: SignInUseCase

    private let signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase

    private let signInWithGoogleUseCase: SignInWithGoogleUseCase

    private let signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase

    private let signInWithKakaoUseCase: SignInWithKakaoUseCase

    private let completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase

    @Published private(set) var uiState = SignInUiState.empty

    let event = PassthroughSubject<SignInEvent, Never>()

    private var signInTask: Task<Void, Never>?

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
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_invalid_credentials"), table: "Localizable")
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func ensureVisitorAccountCompleted(
        email: String,
        nickname: String,
        signupCompleted: Bool
    ) async -> Bool {
        if signupCompleted {
            return true
        }

        do {
            let result = try await completeSignUpForCurrentUserUseCase.invoke(
                email: email,
                nickname: nickname,
                role: .visitor,
                affiliatedCafeId: nil,
                phoneNumber: nil
            )
            return result is AppResultSuccess<AnyObject>
        } catch {
            return false
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
                        uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_invalid_credentials"), table: "Localizable")
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

                    if let success = result as? AppResultSuccess<AnyObject>,
                       let user = success.data as? Shared.User {
                        let userEmail = String(user.email)
                        let userNickname = String(user.nickname)
                        let isCompleted = await ensureVisitorAccountCompleted(
                            email: userEmail,
                            nickname: userNickname,
                            signupCompleted: user.signupCompleted
                        )
                        if isCompleted {
                            uiState.isLoading = false
                            event.send(.signedIn)
                        } else {
                            uiState.isLoading = false
                            uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_apple_failed"), table: "Localizable")
                        }
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_apple_failed"), table: "Localizable")
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_apple_failed"), table: "Localizable")
                }
            }
        }
    }

    init(
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase(),
        signInWithGoogleUseCase: SignInWithGoogleUseCase = KoinInitializerKt.resolveSignInWithGoogleUseCase(),
        signInWithAppleIdTokenUseCase: SignInWithAppleIdTokenUseCase = KoinInitializerKt.resolveSignInWithAppleIdTokenUseCase(),
        signInWithKakaoUseCase: SignInWithKakaoUseCase = KoinInitializerKt.resolveSignInWithKakaoUseCase(),
        completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase = KoinInitializerKt.resolveCompleteSignUpForCurrentUserUseCase()
    ) {
        self.signInUseCase = signInUseCase
        self.signInWithSocialProviderUseCase = SignInWithSocialProviderUseCase(signInUseCase: signInUseCase)
        self.signInWithGoogleUseCase = signInWithGoogleUseCase
        self.signInWithAppleIdTokenUseCase = signInWithAppleIdTokenUseCase
        self.signInWithKakaoUseCase = signInWithKakaoUseCase
        self.completeSignUpForCurrentUserUseCase = completeSignUpForCurrentUserUseCase
    }

    deinit {
        signInTask?.cancel()
    }

    private func handleGoogleSignIn() async {
        do {
            let result = try await signInWithGoogleUseCase.invoke()

            if let success = result as? AppResultSuccess<AnyObject>,
               let user = success.data as? Shared.User {
                let isCompleted = await ensureVisitorAccountCompleted(
                    email: String(user.email),
                    nickname: String(user.nickname),
                    signupCompleted: user.signupCompleted
                )
                if isCompleted {
                    uiState.isLoading = false
                    event.send(.signedIn)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_google_failed"), table: "Localizable")
                }
            } else {
                uiState.isLoading = false
                uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_google_failed"), table: "Localizable")
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_google_failed"), table: "Localizable")
        }
    }

    private func handleKakaoSignIn() async {
        do {
            let result = try await signInWithKakaoUseCase.invoke()

            if let success = result as? AppResultSuccess<AnyObject>,
               let user = success.data as? Shared.User {
                let userNickname = String(user.nickname)
                let completionNickname = userNickname.isEmpty
                    ? String(localized: String.LocalizationValue("signin_default_kakao_nickname"), table: "Localizable")
                    : userNickname

                if await ensureVisitorAccountCompleted(
                    email: String(user.email),
                    nickname: completionNickname,
                    signupCompleted: user.signupCompleted
                ) {
                    uiState.isLoading = false
                    event.send(.signedIn)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_kakao_failed"), table: "Localizable")
                }
            } else {
                uiState.isLoading = false
                uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_kakao_failed"), table: "Localizable")
            }
        } catch {
            if Task.isCancelled { return }
            uiState.isLoading = false
            uiState.errorMessage = String(localized: String.LocalizationValue("signin_error_kakao_failed"), table: "Localizable")
        }
    }
}
