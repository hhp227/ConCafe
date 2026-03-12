//
//  FanManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
final class FanManagementViewModel: ObservableObject {
    private let getFanManagementDataUseCase: GetFanManagementDataUseCase

    private let observeCastVersionUseCase: ObserveCastVersionUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = FanManagementUiState.empty

    let event = PassthroughSubject<FanManagementEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func observeSession() {
        watchHandles[.session]?.cancel()
        watchHandles[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                self.unbindCastVersion()
                self.loadFanManagement()
            }
        }
    }

    private func bindCastVersion(_ castId: String) {
        watchHandles[.castVersion]?.cancel()

        var isInitialEmission = true
        watchHandles[.castVersion] = observeCastVersionUseCase.watch(castId: castId) { [weak self] _ in
            guard let self else { return }
            if isInitialEmission {
                isInitialEmission = false
                return
            }
            Task { @MainActor in
                self.loadFanManagement()
            }
        }
    }

    private func unbindCastVersion() {
        watchHandles.removeValue(forKey: .castVersion)?.cancel()
    }

    private func setInfoMessage(_ message: String) {
        uiState.infoMessage = message
    }

    private func loadFanManagement() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil
        uiState.infoMessage = nil

        loadTask = Task {
            do {
                let result = try await getFanManagementDataUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.FanManagementData {
                    let cast = data.detail.cast
                    bindCastVersion(cast.id)
                    uiState = FanManagementUiState(
                        isLoading: false,
                        errorMessage: nil,
                        fanManagementData: data,
                        stats: [
                            .init(label: "전체 팔로워", value: "\(data.followers.count)", highlight: .standard),
                            .init(label: "근무 일정", value: "\(data.detail.schedule.count)", highlight: .primary),
                            .init(label: "평점", value: String(format: "%.1f", cast.rating), highlight: .standard)
                        ],
                        recentFollowers: Array(data.followers.prefix(10)).enumerated().map { index, user in
                            FanManagementUiState.RecentFollower(
                                id: user.id,
                                name: user.nickname,
                                joinedLabel: {
                                    switch index {
                                    case 0: return "방금 전"
                                    case 1: return "2시간 전"
                                    case 2: return "5시간 전"
                                    default: return "최근"
                                    }
                                }(),
                                accent: index == 0
                            )
                        },
                        topFans: [],
                        infoMessage: nil
                    )
                } else {
                    unbindCastVersion()
                    uiState = .empty
                    uiState.isLoading = false
                    uiState.errorMessage = "팬관리 데이터를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastVersion()
                uiState = .empty
                uiState.isLoading = false
                uiState.errorMessage = "팬관리 데이터를 불러오지 못했습니다."
            }
        }
    }

    private func clickQuickAction(_ quickAction: FanManagementUiState.QuickAction) {
        switch quickAction {
        case .workSchedule:
            event.send(.navigateToSchedule)
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
            guard let detail = uiState.fanManagementData?.detail else { return }
            event.send(.navigateToCastEdit(cafeId: detail.cast.cafeId, castId: detail.cast.id))
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

    init(
        getFanManagementDataUseCase: GetFanManagementDataUseCase = KoinInitializerKt.resolveGetFanManagementDataUseCase(),
        observeCastVersionUseCase: ObserveCastVersionUseCase = KoinInitializerKt.resolveObserveCastVersionUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getFanManagementDataUseCase = getFanManagementDataUseCase
        self.observeCastVersionUseCase = observeCastVersionUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
    }

    deinit {
        loadTask?.cancel()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case castVersion
    }
}
