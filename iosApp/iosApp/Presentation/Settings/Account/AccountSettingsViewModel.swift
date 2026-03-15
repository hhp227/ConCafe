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

    private var loadTask: Task<Void, Never>?

    private var sessionWatchHandle: WatchHandle?

    private func observeSession() {
        sessionWatchHandle?.cancel()
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                self.loadAccountSettings()
            }
        }
    }

    private func loadAccountSettings() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        loadTask = Task {
            do {
                let result = try await getMyInfoUseCase.invoke()
                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.MyInfoFeed {
                    uiState = feed.toUiState()
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
        if uiState.nickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            emitMessage("닉네임을 입력해 주세요.")
        } else if !uiState.email.contains("@") {
            emitMessage("올바른 이메일 형식을 입력해 주세요.")
        } else {
            emitMessage("계정 기본 정보를 저장했어요. 현재 단계에서는 로컬 상태에 반영됩니다.")
        }
    }

    private func openCastEdit() {
        guard uiState.castId?.isEmpty == false else {
            emitMessage("연결된 캐스트 프로필이 아직 없습니다.")
            return
        }
        event.send(.navigateToCastEdit(cafeId: uiState.castCafeId, castId: uiState.castId))
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
            uiState.nickname = value
        case .emailChanged(let value):
            uiState.email = value
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

    deinit {
        loadTask?.cancel()
        sessionWatchHandle?.cancel()
    }

    private let deleteConfirmationText = "탈퇴"
}

private extension Shared.MyInfoFeed {
    func toUiState() -> AccountSettingsUiState {
        let currentCast = castDetail?.cast
        return AccountSettingsUiState(
            isLoading: false,
            errorMessage: nil,
            nickname: user?.nickname ?? "",
            email: user?.email ?? "",
            role: user?.role,
            memberSince: user?.createdAt ?? "",
            roleSummary: {
                switch user?.role {
                case .cast:
                    return "캐스트 계정으로 팬과의 접점을 관리하고 있어요."
                case .cafeOwner:
                    return "운영 카페와 함께 계정 권한을 관리하고 있어요."
                case .admin:
                    return "운영 관리용 관리자 계정입니다."
                case .visitor:
                    return "팬 활동과 리뷰 기록을 관리하는 일반 계정입니다."
                default:
                    return "로그인이 필요한 화면입니다."
                }
            }(),
            linkedCafeName: castDetail?.cafe.name,
            ownedCafeCount: Int(ownedCafes.count),
            castId: currentCast?.id,
            castCafeId: currentCast?.cafeId,
            castName: currentCast?.name ?? "",
            castConceptRole: currentCast?.conceptRole ?? "",
            castDescription: currentCast?.desc ?? "",
            isDeleteDialogVisible: false,
            deleteConfirmation: "",
            isDeleteRequested: false
        )
    }
}
