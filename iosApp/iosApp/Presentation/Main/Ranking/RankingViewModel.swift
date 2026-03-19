//
//  RankingViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class RankingViewModel: ObservableObject {
    private let getRankingFeedUseCase: GetRankingFeedUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    @Published private(set) var uiState = RankingUiState.empty

    let event = PassthroughSubject<RankingEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    uiState.isLoggedIn = user != nil
                    if user != nil {
                        uiState.isLoginPromptVisible = false
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadRankingFeed() {
        let period = uiState.selectedPeriod
        let country = uiState.selectedRegion.country
        let city = uiState.selectedRegion.city

        uiState.isLoading = true
        uiState.errorMessage = nil
        Task {
            do {
                let result = try await getRankingFeedUseCase.invoke(period: period, country: country, city: city)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.RankingFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.selectedAdIndex = 0
                    uiState.ads = feed.ads
                    uiState.maidRankings = feed.castRankings
                    uiState.cafeRankings = feed.cafeRankings
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "랭킹 데이터를 불러오지 못했습니다."
                }
            } catch {
                uiState.isLoading = false
                uiState.errorMessage = "랭킹 데이터를 불러오지 못했습니다."
            }
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                        self.patchCafeRanking(updated.cafe)
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
                        self.patchCastRanking(updated.cast)
                    case let deleted as Shared.CastEvent.Deleted:
                        self.uiState.maidRankings.removeAll { $0.id == deleted.castId }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchCafeRanking(_ cafe: Cafe) {
        let subtitle = cafe.region.address.components(separatedBy: "구").first?
            .components(separatedBy: "로").first?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let nextSubtitle = (subtitle?.isEmpty == false ? subtitle! : cafe.region.city)
        uiState.cafeRankings = uiState.cafeRankings.map { entry in
            guard entry.id == cafe.id else { return entry }
            return RankingFeedEntry(
                id: entry.id,
                rank: entry.rank,
                name: cafe.name,
                subtitle: nextSubtitle,
                score: entry.score,
                change: entry.change,
                startColorHex: entry.startColorHex,
                endColorHex: entry.endColorHex,
                symbol: entry.symbol
            )
        }
    }

    private func patchCastRanking(_ cast: Cast) {
        uiState.maidRankings = uiState.maidRankings.map { entry in
            guard entry.id == cast.id else { return entry }
            return RankingFeedEntry(
                id: entry.id,
                rank: entry.rank,
                name: cast.name,
                subtitle: entry.subtitle,
                score: entry.score,
                change: entry.change,
                startColorHex: entry.startColorHex,
                endColorHex: entry.endColorHex,
                symbol: entry.symbol
            )
        }
    }

    private func requireSignedIn(onAuthenticated: @escaping () -> Void) {
        if uiState.isLoggedIn {
            onAuthenticated()
        } else {
            uiState.isLoginPromptVisible = true
        }
    }

    func onAction(_ action: RankingAction) {
        switch action {
        case .changeTab(let tab):
            uiState.selectedTab = tab
        case .changePeriod(let period):
            uiState.selectedPeriod = period
            loadRankingFeed()
        case .changeRegion(let region):
            uiState.selectedRegion = region
            loadRankingFeed()
        case .selectAd(let index):
            let lastIndex = max(uiState.ads.count - 1, 0)
            uiState.selectedAdIndex = min(max(index, 0), lastIndex)
        case .tapMaid(let id):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCast(id: id))
            }
        case .tapCafe(let id):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCafe(id: id))
            }
        case .loginPromptSignInTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        }
    }

    init(
        getRankingFeedUseCase: GetRankingFeedUseCase = KoinInitializerKt.resolveGetRankingFeedUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher()
    ) {
        self.getRankingFeedUseCase = getRankingFeedUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher
        
        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        loadRankingFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case cafeDetailEvent
        case castEvent
    }
}
