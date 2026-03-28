//
//  MyInfoViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class MyInfoViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    private let visitEventPublisher: VisitEventPublisher

    private let userEventPublisher: UserEventPublisher

    @Published private(set) var uiState = MyInfoUiState.empty

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.loadMyInfo()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }
    
    private func loadMyInfo() {
        tasks[.loadMyInfo]?.cancel()

        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.loadMyInfo] = Task {
            do {
                let result = try await getMyInfoUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                let feed = success.data as? Shared.MyInfoFeed {
                    let normalizedRecentVisits = normalizeCafes(
                        feed.recentVisits,
                        maxCount: feed.recentVisits.count
                    )
                    let normalizedFavorites = normalizeCafes(
                        feed.favorites,
                        maxCount: feed.favorites.count
                    )
                    uiState = MyInfoUiState(
                        isLoading: false,
                        errorMessage: nil,
                        isLoggedIn: feed.isLoggedIn,
                        user: feed.user,
                        summary: feed.summary,
                        castDetail: feed.castDetail,
                        ownedCafes: feed.ownedCafes,
                        badges: feed.badges,
                        popularCafes: feed.popularCafes,
                        recentVisits: normalizedRecentVisits,
                        favorites: normalizedFavorites,
                        followedMaids: feed.followedMaids,
                        isLoginPromptVisible: false
                    )
                } else if let failure = result as? AppResultFailure {
                    uiState = .empty
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState = .empty
                }
            } catch {
                if Task.isCancelled { return }

                uiState = .empty
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                        self.patchCafe(updated.cafe)
                    } else if event is CafeDetailEvent.FavoriteToggled {
                        self.loadMyInfo()
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeVisitEvent() {
        tasks[.visitEvent]?.cancel()
        tasks[.visitEvent] = Task {
            do {
                for try await event in asyncSequence(for: visitEventPublisher.events) {
                    switch event {
                    case is VisitEvent.Created:
                        self.applyVisitCountDelta(1)
                    case is VisitEvent.Deleted:
                        self.applyVisitCountDelta(-1)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let updated as Shared.CastEvent.Updated:
                        if let isFollowing = updated.isFollowing?.boolValue {
                            self.updateFollowedCast(updated.cast, isFollowing: isFollowing)
                        } else {
                            self.patchCast(updated.cast)
                        }
                    case let deleted as Shared.CastEvent.Deleted:
                        self.removeCast(deleted.castId)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeUserEvent() {
        tasks[.userEvent]?.cancel()
        tasks[.userEvent] = Task {
            do {
                for try await event in asyncSequence(for: userEventPublisher.events) {
                    switch event {
                    case let updated as Shared.UserEvent.ProfileUpdated:
                        self.patchUser(updated.user)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchCafe(_ cafe: Cafe) {
        uiState.ownedCafes = uiState.ownedCafes.map { item in
            guard item.id == cafe.id else { return item }
            return CafeManagementData.OwnedCafeSummary(
                id: item.id,
                name: cafe.name,
                city: cafe.region.city,
                isApproved: item.isApproved,
                todayVisitors: item.todayVisitors,
                todayCheckIns: item.todayCheckIns,
                todayReviews: item.todayReviews,
                rating: cafe.ratingAvg,
                castCount: item.castCount,
                noticeCount: item.noticeCount,
                externalLinkCount: item.externalLinkCount,
                thumbnailImage: cafe.thumbnailImage
            )
        }
        uiState.popularCafes = uiState.popularCafes.map { $0.id == cafe.id ? cafe : $0 }
        uiState.recentVisits = uiState.recentVisits.map { $0.id == cafe.id ? cafe : $0 }
        uiState.favorites = uiState.favorites.map { $0.id == cafe.id ? cafe : $0 }
        if let detail = uiState.castDetail, detail.cafe.id == cafe.id {
            uiState.castDetail = CastDetail(
                cast: detail.cast,
                cafe: cafe,
                images: detail.images,
                schedule: detail.schedule,
                visitCertificationCount: detail.visitCertificationCount
            )
        }
    }

    private func patchCast(_ cast: Cast) {
        uiState.followedMaids = uiState.followedMaids.map { $0.id == cast.id ? cast : $0 }
        if let detail = uiState.castDetail, detail.cast.id == cast.id {
            uiState.castDetail = CastDetail(
                cast: cast,
                cafe: detail.cafe,
                images: detail.images,
                schedule: detail.schedule,
                visitCertificationCount: detail.visitCertificationCount
            )
        }
    }

    private func removeCast(_ castId: String) {
        uiState.followedMaids.removeAll { $0.id == castId }
        if uiState.castDetail?.cast.id == castId {
            uiState.castDetail = nil
        }
    }

    private func patchUser(_ user: User) {
        if uiState.user?.id == user.id {
            uiState.user = user
        }
    }

    private func updateFollowedCast(_ cast: Cast, isFollowing: Bool) {
        if isFollowing {
            uiState.followedMaids = upsertFollowedCast(uiState.followedMaids, cast: cast)
        } else {
            uiState.followedMaids.removeAll { $0.id == cast.id }
        }
        if let summary = uiState.summary {
            uiState.summary = MyPageSummary(
                userId: summary.userId,
                totalVisits: summary.totalVisits,
                favoritesCount: summary.favoritesCount,
                followedCastsCount: Int32(uiState.followedMaids.count),
                badgesCount: summary.badgesCount,
                level: summary.level
            )
        }
        if let detail = uiState.castDetail, detail.cast.id == cast.id {
            uiState.castDetail = CastDetail(
                cast: cast,
                cafe: detail.cafe,
                images: detail.images,
                schedule: detail.schedule,
                visitCertificationCount: detail.visitCertificationCount
            )
        }
    }

    private func upsertFollowedCast(_ items: [Cast], cast: Cast) -> [Cast] {
        var result = items
        if let index = result.firstIndex(where: { $0.id == cast.id }) {
            result[index] = cast
        } else {
            result.insert(cast, at: 0)
        }
        return result
    }

    private func applyVisitCountDelta(_ delta: Int) {
        guard delta != 0, let summary = uiState.summary else { return }

        let nextVisitCount = max(Int(summary.totalVisits) + delta, 0)
        let nextStampCount = max(Int(summary.badgesCount) + delta, 0)
        let nextLevel = max(1, 1 + (nextVisitCount / 5))
        uiState.summary = MyPageSummary(
            userId: summary.userId,
            totalVisits: Int32(nextVisitCount),
            favoritesCount: summary.favoritesCount,
            followedCastsCount: summary.followedCastsCount,
            badgesCount: Int32(nextStampCount),
            level: Int32(nextLevel)
        )
        let favoritesCount = Int(summary.favoritesCount)
        let followedCount = Int(summary.followedCastsCount)
        uiState.badges = uiState.badges.map { badge in
            let unlocked: Bool

            switch badge.id {
            case "badge-checkin-starter":
                unlocked = nextStampCount >= 1
            case "badge-stamp-collector":
                unlocked = nextStampCount >= 3
            case "badge-regular-visitor":
                unlocked = nextVisitCount >= 5
            case "badge-checkin-veteran":
                unlocked = nextVisitCount >= 10
            case "badge-favorite-curator":
                unlocked = favoritesCount >= 3
            case "badge-favorite-master":
                unlocked = favoritesCount >= 10
            case "badge-cast-supporter":
                unlocked = followedCount >= 3
            case "badge-cast-ambassador":
                unlocked = followedCount >= 10
            case "badge-level-up":
                unlocked = nextLevel >= 3
            case "badge-concafe-master":
                unlocked = nextStampCount >= 10
            default:
                unlocked = badge.unlocked
            }
            return ProfileBadge(
                id: badge.id,
                name: badge.name,
                icon: badge.icon,
                unlocked: unlocked
            )
        }
    }

    func onAction(_ action: MyInfoAction) {
        switch action {
        case .cafeTapped(let id):
            if uiState.isLoggedIn {
                event.send(.navigateToCafe(id: id))
            } else {
                uiState.isLoginPromptVisible = true
            }
        case .maidTapped(let id):
            if uiState.isLoggedIn {
                event.send(.navigateToCast(id: id))
            } else {
                uiState.isLoginPromptVisible = true
            }
        case .loginPromptSignInTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        case .signInTapped:
            event.send(.navigateToSignIn)
        case .refresh:
            loadMyInfo()
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        visitEventPublisher: VisitEventPublisher = KoinInitializerKt.resolveVisitEventPublisher(),
        userEventPublisher: UserEventPublisher = KoinInitializerKt.resolveUserEventPublisher()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher
        self.visitEventPublisher = visitEventPublisher
        self.userEventPublisher = userEventPublisher

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        observeVisitEvent()
        observeUserEvent()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case loadMyInfo
        case session
        case cafeDetailEvent
        case castEvent
        case visitEvent
        case userEvent
    }

    private func normalizeCafes(_ cafes: [Cafe], maxCount: Int) -> [Cafe] {
        var seen = Set<String>()
        var normalized: [Cafe] = []

        for cafe in cafes {
            if seen.contains(cafe.id) { continue }
            seen.insert(cafe.id)
            normalized.append(cafe)
            if maxCount > 0 && normalized.count >= maxCount {
                break
            }
        }
        return normalized
    }
}
