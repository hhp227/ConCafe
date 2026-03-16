//
//  RankingViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class RankingViewModel: ObservableObject {
    private let getRankingFeedUseCase: GetRankingFeedUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    @Published private(set) var uiState = RankingUiState.empty

    let event = PassthroughSubject<RankingEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func loadRankingFeed() {
        let period = uiState.selectedPeriod
        let country = uiState.selectedRegion.country
        let city = uiState.selectedRegion.city

        uiState.isLoading = true
        uiState.errorMessage = nil
        loadTask?.cancel()
        loadTask = Task {
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
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                    self.patchCafeRanking(updated.cafe)
                }
            }
        }
    }

    private func observeCastEvent() {
        watchHandles[.castEvent]?.cancel()
        watchHandles[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let updated as Shared.CastEvent.Updated:
                    self.patchCastRanking(updated.cast)
                case let deleted as Shared.CastEvent.Deleted:
                    self.uiState.maidRankings.removeAll { $0.id == deleted.castId }
                default:
                    break
                }
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
            event.send(.navigateToCast(id: id))
        case .tapCafe(let id):
            event.send(.navigateToCafe(id: id))
        }
    }

    init(
        getRankingFeedUseCase: GetRankingFeedUseCase = KoinInitializerKt.resolveGetRankingFeedUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.getRankingFeedUseCase = getRankingFeedUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase
        
        observeCafeDetailEvent()
        observeCastEvent()
        loadRankingFeed()
    }

    deinit {
        loadTask?.cancel()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case cafeDetailEvent
        case castEvent
    }
}
