//
//  AccountSettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class AccountSettingsViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let deleteAccountUseCase: DeleteAccountUseCase

    private let updateUserProfileUseCase: UpdateUserProfileUseCase

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
                case .google, .kakao:
                    result = try await deleteAccountUseCase.invoke(
                        request: DeleteAccountRequest(
                            provider: uiState.authProvider,
                            password: nil,
                            idToken: nil
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
    }

    private enum TaskKey {
        case observeSession
        case loadAccountSettings
        case saveUserInfo
        case deleteAccount
    }
}
