//
//  FanManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine

@MainActor
final class FanManagementViewModel: ObservableObject {
    @Published private(set) var uiState = FanManagementUiState()

    let event = PassthroughSubject<FanManagementEvent, Never>()

    private func setInfoMessage(_ message: String) {
        uiState.infoMessage = message
    }

    private func clickQuickAction(_ quickAction: FanManagementUiState.QuickAction) {
        switch quickAction {
        case .workSchedule:
            setInfoMessage("출근 관리 화면 연결은 다음 단계에서 구현합니다.")
        }
    }

    private func clickRecentFollower(_ id: String) {
        guard let follower = uiState.recentFollowers.first(where: { $0.id == id }) else { return }
        setInfoMessage("\(follower.name) 팬 상세 화면은 다음 단계에서 연결합니다.")
    }

    private func clickTopFan(_ id: String) {
        guard let fan = uiState.topFans.first(where: { $0.id == id }) else { return }
        setInfoMessage("\(fan.name) 활동 리포트는 다음 단계에서 제공합니다.")
    }

    func onAction(_ action: FanManagementAction) {
        switch action {
        case .clickEditProfile:
            event.send(.navigateToCastEdit(cafeId: uiState.castProfile.cafeId, castId: uiState.castProfile.castId))
        case .clickNotification:
            event.send(.showMessage("새 알림 \(uiState.notificationCount)건이 있습니다."))
        case .clickPrimaryAnnouncement:
            setInfoMessage("팬 공지 작성 흐름은 다음 단계에서 연결합니다.")
        case .clickQuickAction(let quickAction):
            clickQuickAction(quickAction)
        case .clickViewAllFollowers:
            setInfoMessage("전체 팔로워 목록은 다음 단계에서 제공합니다.")
        case .clickRecentFollower(let id):
            clickRecentFollower(id)
        case .clickTopFan(let id):
            clickTopFan(id)
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }
}
