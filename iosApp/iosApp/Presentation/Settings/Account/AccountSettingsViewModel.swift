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

    @Published private(set) var uiState = AccountSettingsUiState.empty

    let event = PassthroughSubject<AccountSettingsEvent, Never>()

    private var deleteAccountTask: Task<Void, Never>?

    private func observeSession() {
        Task {
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

        Task {
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
        if uiState.nicknameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            emitMessage("닉네임을 입력해 주세요.")
        } else {
            emitMessage("계정 기본 정보를 저장했어요. 현재 단계에서는 로컬 상태에 반영됩니다.")
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
    }

    private func dismissDeleteDialog() {
        uiState.isDeleteDialogVisible = false
        uiState.deletePassword = ""
    }

    private func deleteAccount() {
        if uiState.deletePassword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            emitMessage("회원 비밀번호를 입력해 주세요.")
            return
        }

        uiState.isLoading = true
        uiState.errorMessage = nil

        deleteAccountTask?.cancel()
        let password = uiState.deletePassword
        deleteAccountTask = Task {
            do {
                let result = try await deleteAccountUseCase.invoke(password: password)

                if let failure = result as? AppResultFailure {
                    let message = mapDeleteFailureMessage(failure)
                    uiState.isLoading = false
                    uiState.errorMessage = message
                    emitMessage(message)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.isDeleteRequested = true
                    uiState.isDeleteDialogVisible = false
                    uiState.deletePassword = ""
                    event.send(.navigateBack)
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "회원탈퇴에 실패했습니다. 다시 시도해 주세요."
                emitMessage("회원탈퇴에 실패했습니다. 다시 시도해 주세요.")
            }
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
        case .deleteAccountTapped:
            deleteAccount()
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        deleteAccountUseCase: DeleteAccountUseCase = KoinInitializerKt.resolveDeleteAccountUseCase()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.deleteAccountUseCase = deleteAccountUseCase

        loadAccountSettings()
        observeSession()
    }

    deinit {
        deleteAccountTask?.cancel()
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
}
