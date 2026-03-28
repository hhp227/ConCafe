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
                    uiState.errorMessage = "계정 정보를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "계정 정보를 불러오지 못했습니다."
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
            emitMessage("닉네임을 입력해 주세요.")
        } else {
            uiState.isLoading = true
            uiState.errorMessage = nil

            tasks[.saveUserInfo]?.cancel()
            tasks[.saveUserInfo] = Task {
                do {
                    let result = try await updateUserProfileUseCase.invoke(nickname: nicknameInput, profileImage: profileImage)

                    if result is AppResultSuccess<AnyObject> {
                        uiState.isLoading = false
                        emitMessage("계정 기본 정보를 원격 데이터에 저장했어요.")
                        loadAccountSettings()
                    } else if let failure = result as? AppResultFailure {
                        let message = mapProfileUpdateFailureMessage(failure)
                        uiState.isLoading = false
                        uiState.errorMessage = nil
                        emitMessage(message)
                    } else {
                        uiState.isLoading = false
                        uiState.errorMessage = nil
                        emitMessage("프로필 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.")
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    emitMessage("프로필 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.")
                }
            }
        }
    }

    private func openCastEdit() {
        guard let cast = uiState.myInfoFeed?.castDetail?.cast, !cast.id.isEmpty else {
            emitMessage("연결된 캐스트 프로필이 아직 없습니다.")
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
        if uiState.deletePassword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.deletePasswordErrorMessage = "회원 비밀번호를 입력해 주세요."
            return
        }

        uiState.isLoading = true
        uiState.errorMessage = nil

        let password = uiState.deletePassword
        tasks[.deleteAccount]?.cancel()
        tasks[.deleteAccount] = Task {
            do {
                let result = try await deleteAccountUseCase.invoke(password: password)

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
                uiState.errorMessage = "회원탈퇴에 실패했습니다. 다시 시도해 주세요."
                emitMessage("회원탈퇴에 실패했습니다. 다시 시도해 주세요.")
            }
        }
    }

    private func mapDeleteFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("INVALID PASSWORD")
            || rawError.contains("INVALID_LOGIN_CREDENTIALS")
            || rawError.contains("INVALID_PASSWORD")
            || rawError.contains("EMAIL_NOT_FOUND") {
            return "비밀번호가 올바르지 않습니다."
        } else {
            return "회원탈퇴에 실패했습니다. 다시 시도해 주세요."
        }
    }

    private func mapProfileUpdateFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("UNAUTHORIZED") {
            return "로그인이 만료되었습니다. 다시 로그인해 주세요."
        } else if rawError.contains("VALIDATIONFAILED") {
            return "닉네임을 입력해 주세요."
        } else {
            return "프로필 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."
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
