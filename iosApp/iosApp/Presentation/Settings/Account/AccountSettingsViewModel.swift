//
//  AccountSettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared

@MainActor
final class AccountSettingsViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = AccountSettingsUiState.empty

    let event = PassthroughSubject<AccountSettingsEvent, Never>()

    private func observeSession() {
        Task {
            
        }
        /*observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                self.loadAccountSettings()
            }
        }*/
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
        uiState.deleteConfirmation = ""
    }

    private func dismissDeleteDialog() {
        uiState.isDeleteDialogVisible = false
        uiState.deleteConfirmation = ""
    }

    private func deleteAccount() {
        if uiState.deleteConfirmation != deleteConfirmationText {
            emitMessage("'\(deleteConfirmationText)'를 정확히 입력해 주세요.")
            return
        }
        uiState.isDeleteRequested = true
        uiState.isDeleteDialogVisible = false
        uiState.deleteConfirmation = ""
        emitMessage("회원탈퇴 요청 단계를 진행했어요. 실제 서버 삭제 연동은 후속 단계에서 연결됩니다.")
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
        case .deleteConfirmationChanged(let value):
            uiState.deleteConfirmation = value
        case .deleteAccountTapped:
            deleteAccount()
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        loadAccountSettings()
        observeSession()
    }

    private let deleteConfirmationText = "탈퇴"
}
