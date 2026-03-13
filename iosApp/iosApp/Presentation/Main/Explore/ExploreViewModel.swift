//
//  ExploreViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
class ExploreViewModel: ObservableObject {
    private let getExploreFeedUseCase: GetExploreFeedUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    @Published private(set) var uiState = ExploreUiState.empty

    let event = PassthroughSubject<ExploreEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func loadExploreFeed() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        let query = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let queryOrNil = query.isEmpty ? nil : query

        loadTask = Task {
            do {
                let result = try await getExploreFeedUseCase.invoke(
                    query: queryOrNil,
                    regionKey: uiState.selectedRegion.rawValue,
                    sortKey: uiState.selectedSort.rawValue,
                    pageSize: 50
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                let feed = success.data as? Shared.ExploreFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.cafes = feed.cafes
                    uiState.maids = feed.maids
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "\(failure.error)"
                    uiState.cafes = []
                    uiState.maids = []
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "unknown"
                    uiState.cafes = []
                    uiState.maids = []
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
                uiState.cafes = []
                uiState.maids = []
            }
        }
    }

    private func observeCafeDetailEvent() {
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                    self.patchCafe(updated.cafe)
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
                case let created as Shared.CastEvent.Created:
                    self.addCastIfVisible(created.cast)
                case let updated as Shared.CastEvent.Updated:
                    self.patchCast(updated.cast)
                case let deleted as Shared.CastEvent.Deleted:
                    self.uiState.maids.removeAll { $0.id == deleted.castId }
                default:
                    break
                }
            }
        }
    }

    private func patchCafe(_ cafe: Cafe) {
        let cafeMatches = matchesCafeFilters(cafe)
        uiState.cafes = uiState.cafes.compactMap { item in
            guard item.id == cafe.id else { return item }
            return cafeMatches ? cafe : nil
        }.sortedCafes(by: uiState.selectedSort)
        uiState.maids = uiState.maids.filter { maid in
            guard maid.cafeId == cafe.id else { return true }
            return matchesCastFilters(maid, cafeVisible: cafeMatches)
        }.sortedCasts(by: uiState.selectedSort)
    }

    private func addCastIfVisible(_ cast: Cast) {
        guard !uiState.maids.contains(where: { $0.id == cast.id }), matchesCastFilters(cast) else { return }
        uiState.maids = (uiState.maids + [cast]).sortedCasts(by: uiState.selectedSort)
    }

    private func patchCast(_ cast: Cast) {
        uiState.maids = uiState.maids.compactMap { item in
            guard item.id == cast.id else { return item }
            return matchesCastFilters(cast) ? cast : nil
        }.sortedCasts(by: uiState.selectedSort)
    }

    private func matchesCafeFilters(_ cafe: Cafe) -> Bool {
        let query = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let matchesQuery = query.isEmpty || cafe.name.localizedCaseInsensitiveContains(query)
        let matchesRegion: Bool
        switch uiState.selectedRegion {
        case .all:
            matchesRegion = true
        case .seoul:
            matchesRegion = cafe.region.country.caseInsensitiveCompare("KR") == .orderedSame
                && cafe.region.city.caseInsensitiveCompare("Seoul") == .orderedSame
        case .tokyo:
            matchesRegion = cafe.region.country.caseInsensitiveCompare("JP") == .orderedSame
                && cafe.region.city.caseInsensitiveCompare("Tokyo") == .orderedSame
        case .osaka:
            matchesRegion = cafe.region.country.caseInsensitiveCompare("JP") == .orderedSame
                && cafe.region.city.caseInsensitiveCompare("Osaka") == .orderedSame
        }
        return matchesQuery && matchesRegion
    }

    private func matchesCastFilters(_ cast: Cast, cafeVisible: Bool? = nil) -> Bool {
        let query = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let matchesQuery = query.isEmpty || cast.name.localizedCaseInsensitiveContains(query)
        let matchesRegion = cafeVisible ?? (uiState.selectedRegion == .all || uiState.cafes.contains(where: { $0.id == cast.cafeId }))
        return matchesQuery && matchesRegion
    }

    func onAction(_ action: ExploreAction) {
        switch action {
        case .queryChanged(let query):
            uiState.query = query
            loadExploreFeed()
        case .regionChanged(let region):
            uiState.selectedRegion = region
            loadExploreFeed()
        case .sortChanged(let sort):
            uiState.selectedSort = sort
            loadExploreFeed()
        case .tabChanged(let tab):
            uiState.selectedTab = tab
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .refresh:
            loadExploreFeed()
        }
    }

    init(
        getExploreFeedUseCase: GetExploreFeedUseCase = KoinInitializerKt.resolveGetExploreFeedUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.getExploreFeedUseCase = getExploreFeedUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase

        observeCafeDetailEvent()
        observeCastEvent()
        loadExploreFeed()
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

private extension Array where Element == Cafe {
    func sortedCafes(by sort: ExploreUiState.SortFilter) -> [Cafe] {
        switch sort {
        case .popular:
            return sorted { $0.reviewCount > $1.reviewCount }
        case .latest:
            return sorted { $0.id > $1.id }
        case .rating:
            return sorted { $0.ratingAvg > $1.ratingAvg }
        }
    }
}

private extension Array where Element == Cast {
    func sortedCasts(by sort: ExploreUiState.SortFilter) -> [Cast] {
        switch sort {
        case .popular, .rating:
            return sorted { $0.followerCount > $1.followerCount }
        case .latest:
            return sorted { $0.id > $1.id }
        }
    }
}
