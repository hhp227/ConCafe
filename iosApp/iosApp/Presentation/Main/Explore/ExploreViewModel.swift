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

    @Published private(set) var uiState = ExploreUiState.empty

    let event = PassthroughSubject<ExploreEvent, Never>()

    private var loadTask: Task<Void, Never>?

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
        getExploreFeedUseCase: GetExploreFeedUseCase = KoinInitializerKt.resolveGetExploreFeedUseCase()
    ) {
        self.getExploreFeedUseCase = getExploreFeedUseCase

        loadExploreFeed()
    }

    deinit {
        loadTask?.cancel()
    }
}
