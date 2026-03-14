//
//  BannerEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine

@MainActor
final class BannerEditViewModel: ObservableObject {
    @Published private(set) var uiState = BannerEditUiState()

    let event = PassthroughSubject<BannerEditEvent, Never>()

    private func clickSave() {
        let validationMessage: String?

        if uiState.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "배너 제목을 입력해주세요."
        } else if uiState.subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "서브 문구를 입력해주세요."
        } else if uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "연결 대상 값을 입력해주세요."
        } else {
            validationMessage = nil
        }

        if let validationMessage {
            uiState.infoMessage = validationMessage
            return
        }

        uiState.isSaving = true
        uiState.infoMessage = nil
        uiState.isSaving = false
        uiState.infoMessage = "배너 초안이 저장되었습니다. 실제 업로드 연동은 다음 단계에서 연결됩니다."
        event.send(.showSaveSuccessAlert)
    }

    func onAction(_ action: BannerEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickImagePicker:
            uiState.selectedImageLabel = "banner_cover_mock.png"
            uiState.infoMessage = "이미지 업로드 연결은 다음 단계에서 구현됩니다."
        case .changeTitle(let value):
            uiState.title = value
        case .changeSubtitle(let value):
            uiState.subtitle = value
        case .selectTarget(let target):
            uiState.selectedTarget = target
            uiState.targetValue = ""
        case .changeTargetValue(let value):
            uiState.targetValue = value
        case .changeDisplayDays(let value):
            uiState.displayDays = min(max(value, 1), 10)
        case .clickSave:
            clickSave()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }
}
