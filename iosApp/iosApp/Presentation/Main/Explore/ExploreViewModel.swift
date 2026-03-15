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
    private let getExploreCafePageUseCase: GetExploreCafePageUseCase

    private let getExploreCastPageUseCase: GetExploreCastPageUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    @Published private(set) var uiState = ExploreUiState.empty

    let event = PassthroughSubject<ExploreEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func refreshCurrentTab() {
        tasks[.cafePage]?.cancel()
        tasks[.maidPage]?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil
        uiState.isLoadingMoreCafes = false
        uiState.isLoadingMoreMaids = false

        if uiState.selectedTab == .cafe {
            uiState.cafes = []
            uiState.cafesNextCursor = nil
            uiState.canLoadMoreCafes = false
            loadCafePage(cursor: nil, append: false)
        } else {
            uiState.maids = []
            uiState.maidsNextCursor = nil
            uiState.canLoadMoreMaids = false
            loadMaidPage(cursor: nil, append: false)
        }
    }

    private func loadCafePage(cursor: String?, append: Bool) {
        tasks[.cafePage]?.cancel()
        let query = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let queryOrNil = query.isEmpty ? nil : query

        tasks[.cafePage] = Task {
            uiState.isLoadingMoreCafes = append
            do {
                let result = try await getExploreCafePageUseCase.invoke(
                    query: queryOrNil,
                    regionKey: uiState.selectedRegion.rawValue,
                    sortKey: uiState.selectedSort.rawValue,
                    cursor: cursor,
                    pageSize: 15
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Cafe> {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.cafes = append ? (uiState.cafes + (page.items as! [Cafe])) : (page.items as! [Cafe])
                    uiState.cafesNextCursor = page.nextCursor
                    uiState.canLoadMoreCafes = page.hasNext
                }
                uiState.isLoadingMoreCafes = false
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.isLoadingMoreCafes = false
            }
        }
    }

    private func loadMoreCafes() {
        guard uiState.canLoadMoreCafes,
              !uiState.isLoadingMoreCafes,
              let cursor = uiState.cafesNextCursor else { return }
        loadCafePage(cursor: cursor, append: true)
    }

    private func loadMaidPage(cursor: String?, append: Bool) {
        tasks[.maidPage]?.cancel()
        let query = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
        let queryOrNil = query.isEmpty ? nil : query

        tasks[.maidPage] = Task {
            uiState.isLoadingMoreMaids = append
            do {
                let result = try await getExploreCastPageUseCase.invoke(
                    query: queryOrNil,
                    regionKey: uiState.selectedRegion.rawValue,
                    sortKey: uiState.selectedSort.rawValue,
                    cursor: cursor,
                    pageSize: 15
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Cast> {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.maids = append ? (uiState.maids + (page.items as! [Cast])) : (page.items as! [Cast])
                    uiState.maidsNextCursor = page.nextCursor
                    uiState.canLoadMoreMaids = page.hasNext
                }
                uiState.isLoadingMoreMaids = false
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.isLoadingMoreMaids = false
            }
        }
    }

    private func loadMoreMaids() {
        guard uiState.canLoadMoreMaids,
              !uiState.isLoadingMoreMaids,
              let cursor = uiState.maidsNextCursor else { return }
        loadMaidPage(cursor: cursor, append: true)
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
            refreshCurrentTab()
        case .regionChanged(let region):
            uiState.selectedRegion = region
            refreshCurrentTab()
        case .sortChanged(let sort):
            uiState.selectedSort = sort
            refreshCurrentTab()
        case .tabChanged(let tab):
            uiState.selectedTab = tab
            if tab == .cafe, uiState.cafes.isEmpty {
                refreshCurrentTab()
            } else if tab == .maid, uiState.maids.isEmpty {
                refreshCurrentTab()
            }
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .loadMoreCafes:
            loadMoreCafes()
        case .loadMoreMaids:
            loadMoreMaids()
        case .refresh:
            refreshCurrentTab()
        }
    }

    init(
        getExploreCafePageUseCase: GetExploreCafePageUseCase = KoinInitializerKt.resolveGetExploreCafePageUseCase(),
        getExploreCastPageUseCase: GetExploreCastPageUseCase = KoinInitializerKt.resolveGetExploreCastPageUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.getExploreCafePageUseCase = getExploreCafePageUseCase
        self.getExploreCastPageUseCase = getExploreCastPageUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase

        observeCafeDetailEvent()
        observeCastEvent()
        refreshCurrentTab()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum TaskKey {
        case cafePage
        case maidPage
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
