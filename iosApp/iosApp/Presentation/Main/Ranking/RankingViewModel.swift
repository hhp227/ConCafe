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

    @Published private(set) var uiState = RankingUiState.empty

    let event = PassthroughSubject<RankingEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private func loadRankingFeed() {
        let period = uiState.selectedPeriod.toDomain()
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
                    uiState.ads = feed.ads.map { RankingUiState.PromoAd($0) }
                    uiState.maidRankings = feed.castRankings.map { RankingUiState.RankingEntry($0) }
                    uiState.cafeRankings = feed.cafeRankings.map { RankingUiState.RankingEntry($0) }
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
        getRankingFeedUseCase: GetRankingFeedUseCase = KoinInitializerKt.resolveGetRankingFeedUseCase()
    ) {
        self.getRankingFeedUseCase = getRankingFeedUseCase
        
        loadRankingFeed()
    }
}

private extension RankingUiState.PeriodFilter {
    func toDomain() -> RankingPeriod {
        switch self {
        case .weekly:
            return .weekly
        case .monthly:
            return .monthly
        }
    }
}

private extension RankingUiState.RegionFilter {
    var country: String? {
        switch self {
        case .all:
            return nil
        case .seoul:
            return "KR"
        case .tokyo, .osaka:
            return "JP"
        }
    }

    var city: String? {
        switch self {
        case .all:
            return nil
        case .seoul:
            return "Seoul"
        case .tokyo:
            return "Tokyo"
        case .osaka:
            return "Osaka"
        }
    }
}

private extension RankingUiState.PromoAd {
    init(_ item: Shared.RankingPromoAd) {
        self.init(
            id: item.id,
            badge: item.badge,
            title: item.title,
            subtitle: item.subtitle,
            description: item.description,
            startColorHex: item.startColorHex,
            endColorHex: item.endColorHex,
            symbol: item.symbol
        )
    }
}

private extension RankingUiState.RankingEntry {
    init(_ item: Shared.RankingFeedEntry) {
        self.init(
            id: item.id,
            rank: Int(item.rank),
            name: item.name,
            subtitle: item.subtitle,
            score: Int(item.score),
            change: item.change,
            startColorHex: item.startColorHex,
            endColorHex: item.endColorHex,
            symbol: item.symbol
        )
    }
}
