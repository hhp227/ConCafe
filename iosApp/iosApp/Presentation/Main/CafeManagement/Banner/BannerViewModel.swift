//
//  BannerViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

final class BannerViewModel: ObservableObject {
    private let cafeId: String?

    private let getHomeFeedUseCase: GetHomeFeedUseCase

    private let deleteHomeBannerUseCase: DeleteHomeBannerUseCase

    private let bannerEventPublisher: BannerEventPublisher

    @Published private(set) var uiState = BannerUiState()

    let event = PassthroughSubject<BannerEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadBanners() {
        tasks[.load]?.cancel()
        tasks[.load] = Task {
            do {
                let result = try await getHomeFeedUseCase.invoke(popularCastCursor: nil, nearbyCafeCursor: nil)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? HomeFeed {
                    let mapped = feed.banners
                        .filter { banner in
                            guard let cafeId else { return true }
                            return banner.cafeId == cafeId
                        }
                        .map { $0.toBannerItem() }
                    uiState.banners = mapped
                    if let pendingDeleteBannerId = uiState.pendingDeleteBannerId,
                       !mapped.contains(where: { $0.id == pendingDeleteBannerId }) {
                        uiState.pendingDeleteBannerId = nil
                    }
                } else {
                    event.send(.showMessage("배너 목록을 불러오지 못했습니다."))
                }
            } catch {
                if Task.isCancelled { return }
                event.send(.showMessage("배너 목록을 불러오지 못했습니다."))
            }
        }
    }

    private func observeBannerEvent() {
        tasks[.bannerEvent]?.cancel()
        tasks[.bannerEvent] = Task {
            do {
                for try await event in asyncSequence(for: bannerEventPublisher.events) {
                    switch event {
                    case is Shared.BannerEvent.Created:
                        self.loadBanners()
                    case let deleted as Shared.BannerEvent.Deleted:
                        self.removeBanner(id: deleted.banner.id)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    func onAction(_ action: BannerAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .selectTab(let tab):
            uiState.selectedTab = tab
        case .createBannerTapped:
            event.send(.navigateToBannerEdit(cafeId: cafeId))
        case .editBannerTapped:
            event.send(.showMessage("편집 기능은 아직 연결되지 않았습니다."))
        case .deleteBannerTapped(let id):
            clickDeleteBanner(id: id)
        case .dismissDeleteBannerDialog:
            uiState.pendingDeleteBannerId = nil
        case .confirmDeleteBanner:
            confirmDeleteBanner()
        }
    }

    private func clickDeleteBanner(id: String) {
        let hasBanner = uiState.banners.contains { $0.id == id }
        if !hasBanner {
            return
        }
        uiState.pendingDeleteBannerId = id
    }

    private func confirmDeleteBanner() {
        guard let bannerId = uiState.pendingDeleteBannerId else {
            return
        }

        uiState.pendingDeleteBannerId = nil
        tasks[.delete]?.cancel()
        tasks[.delete] = Task {
            do {
                let result = try await deleteHomeBannerUseCase.invoke(bannerId: bannerId)
                if result is AppResultSuccess<AnyObject> {
                    removeBanner(id: bannerId)
                    event.send(.showMessage("배너를 삭제했습니다."))
                } else if let failure = result as? AppResultFailure {
                    event.send(.showMessage("\(failure.error)"))
                } else {
                    event.send(.showMessage("배너 삭제에 실패했습니다."))
                }
            } catch {
                if Task.isCancelled { return }
                event.send(.showMessage("배너 삭제에 실패했습니다."))
            }
        }
    }

    private func removeBanner(id: String) {
        uiState.banners.removeAll { $0.id == id }
        if uiState.pendingDeleteBannerId == id {
            uiState.pendingDeleteBannerId = nil
        }
    }

    init(
        cafeId: String? = nil,
        getHomeFeedUseCase: GetHomeFeedUseCase = KoinInitializerKt.resolveGetHomeFeedUseCase(),
        deleteHomeBannerUseCase: DeleteHomeBannerUseCase = KoinInitializerKt.resolveDeleteHomeBannerUseCase(),
        bannerEventPublisher: BannerEventPublisher = KoinInitializerKt.resolveBannerEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getHomeFeedUseCase = getHomeFeedUseCase
        self.deleteHomeBannerUseCase = deleteHomeBannerUseCase
        self.bannerEventPublisher = bannerEventPublisher
        observeBannerEvent()
        loadBanners()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }
}

private enum TaskKey {
    case load
    case delete
    case bannerEvent
}

private extension HomeBanner {
    func toBannerItem() -> BannerItem {
        let tab: BannerTab
        switch statusLabel.uppercased() {
        case "SCHEDULED":
            tab = .scheduled
        case "ENDED", "PAUSED":
            tab = .ended
        default:
            tab = .active
        }
        let statusLabel: String
        switch tab {
        case .active:
            statusLabel = "진행 중"
        case .scheduled:
            statusLabel = "예약"
        case .ended:
            statusLabel = "종료"
        }
        let icon: String
        switch targetType {
        case .cafeDetail:
            icon = "cup.and.saucer.fill"
        case .eventDetail:
            icon = "calendar.badge.plus"
        case .notice:
            icon = "megaphone.fill"
        default:
            icon = "globe"
        }
        return BannerItem(
            id: id,
            title: title,
            description: subtitle,
            periodText: "노출 \(displayDays)일",
            statusLabel: statusLabel,
            tab: tab,
            accentHex: startColorHex,
            imageIcon: icon
        )
    }
}
