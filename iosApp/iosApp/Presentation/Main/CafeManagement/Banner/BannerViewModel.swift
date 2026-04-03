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

    private let getHomeBannerManagementUseCase: GetHomeBannerManagementUseCase

    private let deleteHomeBannerUseCase: DeleteHomeBannerUseCase

    private let bannerEventPublisher: BannerEventPublisher

    @Published private(set) var uiState = BannerUiState()

    let event = PassthroughSubject<BannerEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadBanners() {
        tasks[.load]?.cancel()
        tasks[.load] = Task {
            do {
                let result = try await getHomeBannerManagementUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let banners = success.data as? [HomeBanner] {
                    let mapped = banners
                        .map { $0.toBannerItem() }
                    uiState.banners = mapped
                    if let pendingDeleteBannerId = uiState.pendingDeleteBannerId,
                       !mapped.contains(where: { $0.id == pendingDeleteBannerId }) {
                        uiState.pendingDeleteBannerId = nil
                    }
                } else {
                    event.send(.showMessage(MessageKey.bannerLoadFailed))
                }
            } catch {
                if Task.isCancelled { return }
                event.send(.showMessage(MessageKey.bannerLoadFailed))
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
                    case let updated as Shared.BannerEvent.Updated:
                        self.patchBanner(updated.banner)
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

    private func patchBanner(_ updatedBanner: HomeBanner) {
        let shouldShow: Bool
        if let cafeId = cafeId {
            shouldShow = updatedBanner.cafeId == cafeId
        } else {
            shouldShow = true
        }

        if let index = uiState.banners.firstIndex(where: { $0.id == updatedBanner.id }) {
            if shouldShow {
                uiState.banners[index] = updatedBanner.toBannerItem()
            } else {
                uiState.banners.remove(at: index)
            }
        } else if shouldShow {
            uiState.banners.insert(updatedBanner.toBannerItem(), at: 0)
        }

        if let pendingDeleteBannerId = uiState.pendingDeleteBannerId,
           !uiState.banners.contains(where: { $0.id == pendingDeleteBannerId }) {
            uiState.pendingDeleteBannerId = nil
        }
    }

    func onAction(_ action: BannerAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .selectTab(let tab):
            uiState.selectedTab = tab
        case .createBannerTapped:
            event.send(.navigateToBannerEdit(cafeId: cafeId, bannerId: nil))
        case .editBannerTapped(let id):
            clickEditBanner(id: id)
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

    private func clickEditBanner(id: String) {
        guard let banner = uiState.banners.first(where: { $0.id == id }) else {
            return
        }
        event.send(.navigateToBannerEdit(cafeId: banner.cafeId ?? cafeId, bannerId: banner.id))
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
                    event.send(.showMessage(MessageKey.bannerDeleted))
                } else if let failure = result as? AppResultFailure {
                    let _ = failure
                    event.send(.showMessage(MessageKey.bannerDeleteFailed))
                } else {
                    event.send(.showMessage(MessageKey.bannerDeleteFailed))
                }
            } catch {
                if Task.isCancelled { return }
                event.send(.showMessage(MessageKey.bannerDeleteFailed))
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
        getHomeBannerManagementUseCase: GetHomeBannerManagementUseCase = KoinInitializerKt.resolveGetHomeBannerManagementUseCase(),
        deleteHomeBannerUseCase: DeleteHomeBannerUseCase = KoinInitializerKt.resolveDeleteHomeBannerUseCase(),
        bannerEventPublisher: BannerEventPublisher = KoinInitializerKt.resolveBannerEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getHomeBannerManagementUseCase = getHomeBannerManagementUseCase
        self.deleteHomeBannerUseCase = deleteHomeBannerUseCase
        self.bannerEventPublisher = bannerEventPublisher
        observeBannerEvent()
        loadBanners()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum MessageKey {
        static let bannerLoadFailed = "banner_info_load_failed"
        static let bannerDeleted = "banner_info_deleted"
        static let bannerDeleteFailed = "banner_info_delete_failed"
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
        let statusLabelKey: String
        switch tab {
        case .active:
            statusLabelKey = "banner_status_active"
        case .scheduled:
            statusLabelKey = "banner_status_scheduled"
        case .ended:
            statusLabelKey = "banner_status_ended"
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
            cafeId: cafeId,
            title: title,
            description: subtitle,
            periodDays: displayDays,
            statusLabelKey: statusLabelKey,
            tab: tab,
            accentHex: startColorHex,
            imageIcon: icon,
            imageUrl: imageUrl
        )
    }
}
