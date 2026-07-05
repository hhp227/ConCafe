//
//  CastListViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/05.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CastListViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeCastPageUseCase: GetCafeCastPageUseCase

    private let deleteCastUseCase: DeleteCastUseCase

    private let castEventPublisher: CastEventPublisher

    @Published private(set) var uiState = CastListUiState()

    let event = PassthroughSubject<CastListEvent, Never>()

    private var observeCastEventTask: Task<Void, Never>?

    private func loadCastPage(cursor: String?, append: Bool) {
        Task {
            uiState.isLoading = !append && uiState.casts.isEmpty
            uiState.isLoadingMore = append
            if !append {
                uiState.infoMessage = nil
            }

            do {
                let result = try await getCafeCastPageUseCase.invoke(
                    cafeId: cafeId,
                    cursor: cursor,
                    pageSize: getCafeCastPageUseCase.defaultPageSize()
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeCastPreview> {
                    let items = (page.items as? [CafeCastPreview]) ?? []
                    uiState.casts = append ? (uiState.casts + items) : items
                    uiState.nextCursor = page.nextCursor
                    uiState.hasMoreCasts = page.hasNext
                    uiState.isLoading = false
                    uiState.isLoadingMore = false
                } else {
                    uiState.isLoading = false
                    uiState.isLoadingMore = false
                    uiState.infoMessage = "castlist_info_load_failed"
                }
            } catch {
                uiState.isLoading = false
                uiState.isLoadingMore = false
                uiState.infoMessage = "castlist_info_load_failed"
            }
        }
    }

    private func refreshCasts() {
        loadCastPage(cursor: nil, append: false)
    }

    private func clickLoadMoreCasts() {
        guard uiState.hasMoreCasts, !uiState.isLoadingMore else { return }
        loadCastPage(cursor: uiState.nextCursor, append: true)
    }

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func clickAddCast() {
        event.send(.navigateToCastEdit(cafeId: cafeId, castId: nil))
    }

    private func changeSearchQuery(_ value: String) {
        uiState.searchQuery = value
    }

    private func clickCast(_ castId: String) {
        event.send(.navigateToCastEdit(cafeId: cafeId, castId: castId))
    }

    private func clickCastSchedule(_ castId: String) {
        event.send(.navigateToSchedule(castId: castId))
    }

    private func clickDeleteCast(_ castId: String) {
        uiState.deleteTargetCastId = castId
        uiState.infoMessage = nil
    }

    private func dismissDeleteCastDialog() {
        uiState.deleteTargetCastId = nil
    }

    private func confirmDeleteCast() {
        guard let deleteTargetCastId = uiState.deleteTargetCastId else {
            uiState.deleteTargetCastId = nil
            return
        }

        Task {
            do {
                let result = try await deleteCastUseCase.invoke(castId: deleteTargetCastId)

                if result is AppResultSuccess<AnyObject> {
                    uiState.deleteTargetCastId = nil
                    uiState.infoMessage = "castlist_info_cast_deleted"
                } else if let failure = result as? AppResultFailure {
                    uiState.deleteTargetCastId = nil
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.deleteTargetCastId = nil
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func observeCastEvent() {
        observeCastEventTask?.cancel()
        observeCastEventTask = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let event as Shared.CastEvent.Created:
                        if event.cafeId == self.cafeId {
                            self.refreshCasts()
                        }
                    case let event as Shared.CastEvent.Updated:
                        if event.cafeId == self.cafeId {
                            self.uiState.casts = self.uiState.casts.map { preview in
                                guard preview.id == event.cast.id else { return preview }
                                return CafeCastPreview(
                                    id: preview.id,
                                    name: event.cast.name,
                                    isOnShift: preview.isOnShift,
                                    profileImage: event.cast.profileImage
                                )
                            }
                        }
                    case let event as Shared.CastEvent.Deleted:
                        if event.cafeId == self.cafeId {
                            self.uiState.casts.removeAll { $0.id == event.castId }
                            if self.uiState.deleteTargetCastId == event.castId {
                                self.uiState.deleteTargetCastId = nil
                            }
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    func onAction(_ action: CastListAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickAddCast:
            clickAddCast()
        case .changeSearchQuery(let value):
            changeSearchQuery(value)
        case .clickCast(let castId):
            clickCast(castId)
        case .clickCastSchedule(let castId):
            clickCastSchedule(castId)
        case .clickDeleteCast(let castId):
            clickDeleteCast(castId)
        case .confirmDeleteCast:
            confirmDeleteCast()
        case .dismissDeleteCastDialog:
            dismissDeleteCastDialog()
        case .clickLoadMoreCasts:
            clickLoadMoreCasts()
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }

    init(
        cafeId: String,
        getCafeCastPageUseCase: GetCafeCastPageUseCase = KoinInitializerKt.resolveGetCafeCastPageUseCase(),
        deleteCastUseCase: DeleteCastUseCase = KoinInitializerKt.resolveDeleteCastUseCase(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getCafeCastPageUseCase = getCafeCastPageUseCase
        self.deleteCastUseCase = deleteCastUseCase
        self.castEventPublisher = castEventPublisher

        observeCastEvent()
        refreshCasts()
    }

    deinit {
        observeCastEventTask?.cancel()
        observeCastEventTask = nil
    }
}
