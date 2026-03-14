//
//  CafeDashboardViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation
import Combine
import Shared

@MainActor
final class CafeDashboardViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeCastPageUseCase: GetCafeCastPageUseCase

    private let getCafeDashboardUseCase: GetCafeDashboardUseCase

    private let getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase

    private let approveCastClaimUseCase: ApproveCastClaimUseCase

    private let rejectCastClaimUseCase: RejectCastClaimUseCase

    private let deleteCastUseCase: DeleteCastUseCase

    private let observeBannerEventUseCase: ObserveBannerEventUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastClaimEventUseCase: ObserveCastClaimEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    @Published private(set) var uiState = CafeDashboardUiState()

    let event = PassthroughSubject<CafeDashboardEvent, Never>()

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func loadCafeDashboard() {
        Task {
            uiState.isLoading = true
            uiState.infoMessage = nil

            do {
                let result = try await getCafeDashboardUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeDashboardData {
                    uiState.cafe = data
                    uiState.isLoading = false
                    refreshCastPreviews(resetMessage: false)
                    refreshClaimData(resetMessage: false)
                } else if let failure = result as? AppResultFailure {
                    uiState.cafe = nil
                    uiState.castPreviews = []
                    uiState.pendingCastClaims = []
                    uiState.nextCastCursor = nil
                    uiState.hasMoreCasts = false
                    uiState.isLoading = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.cafe = nil
                uiState.castPreviews = []
                uiState.pendingCastClaims = []
                uiState.nextCastCursor = nil
                uiState.hasMoreCasts = false
                uiState.isLoading = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func loadCastPage(cursor: String?, pageSize: Int, append: Bool) {
        Task {
            uiState.isLoadingMoreCasts = append

            do {
                let result = try await self.getCafeCastPageUseCase.invoke(
                    cafeId: cafeId,
                    cursor: cursor,
                    pageSize: Int32(pageSize)
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeCastPreview> {
                    let items = (page.items as? [CafeCastPreview]) ?? []
                    let mergedItems = append ? (uiState.castPreviews + items) : items
                    uiState.castPreviews = mergedItems
                    if let selectedCastId = uiState.selectedCastId,
                       !mergedItems.contains(where: { $0.id == selectedCastId }) {
                        uiState.selectedCastId = nil
                    }
                    uiState.nextCastCursor = page.nextCursor
                    uiState.hasMoreCasts = page.hasNext
                    uiState.isLoadingMoreCasts = false
                } else {
                    uiState.isLoadingMoreCasts = false
                    uiState.infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
                }
            } catch {
                uiState.isLoadingMoreCasts = false
                uiState.infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
            }
        }
    }

    private func refreshCastPreviews(resetMessage: Bool = true) {
        if resetMessage {
            uiState.infoMessage = nil
        }
        loadCastPage(
            cursor: nil,
            pageSize: Int(getCafeCastPageUseCase.defaultPageSize()),
            append: false
        )
    }

    private func clickLoadMoreCasts() {
        guard uiState.hasMoreCasts, !uiState.isLoadingMoreCasts else { return }
        loadCastPage(
            cursor: uiState.nextCastCursor,
            pageSize: Int(getCafeCastPageUseCase.defaultPageSize()),
            append: true
        )
    }

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func clickShortcut(_ shortcut: CafeDashboardShortcut) {
        switch shortcut {
        case .cafeSettings:
            event.send(.navigateToCafeInfoEdit(cafeId: cafeId))
        case .homeBanner:
            event.send(.navigateToBannerEdit)
        case .eventManagement:
            event.send(.navigateToNoticeEvent(cafeId: cafeId))
        case .menuGoods:
            event.send(.navigateToMenuGoods(cafeId: cafeId))
        case .castSchedule:
            guard let selectedCastId = uiState.selectedCastId else {
                uiState.infoMessage = "출근표를 관리할 캐스트를 목록에서 선택해 주세요."
                return
            }
            event.send(.navigateToSchedule(castId: selectedCastId))
        case .castManagement:
            event.send(.navigateToCastEdit(cafeId: cafeId, castId: nil))
        case .externalLinks:
            uiState.isExternalLinkSheetVisible = true
            uiState.infoMessage = nil
        }
    }

    private func dismissExternalLinkSheet() {
        uiState.isExternalLinkSheetVisible = false
        uiState.externalLinkTitle = ""
        uiState.externalLinkUrl = ""
    }

    private func changeExternalLinkTitle(_ value: String) {
        uiState.externalLinkTitle = value
    }

    private func changeExternalLinkUrl(_ value: String) {
        uiState.externalLinkUrl = value
    }

    private func submitExternalLink() {
        guard uiState.isExternalLinkSubmitEnabled else {
            uiState.infoMessage = "제목과 링크 URL을 모두 입력해 주세요."
            return
        }

        let title = uiState.externalLinkTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let url = uiState.externalLinkUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        uiState.isExternalLinkSheetVisible = false
        uiState.externalLinks.insert(
            CafeDashboardExternalLink(
                id: "external-link-\(UUID().uuidString)",
                title: title,
                url: url
            ),
            at: 0
        )
        uiState.externalLinkTitle = ""
        uiState.externalLinkUrl = ""
        uiState.infoMessage = "외부 링크를 추가했습니다."
    }

    private func clickExternalLinkItem(_ linkId: String) {
        guard let link = uiState.externalLinks.first(where: { $0.id == linkId }) else { return }
        event.send(.navigateToExternalLink(title: link.title, url: link.url))
    }

    private func clickDeleteExternalLink(_ linkId: String) {
        uiState.externalLinks.removeAll { $0.id == linkId }
        uiState.infoMessage = "외부 링크를 삭제했습니다."
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func clickCastSchedule(_ castId: String) {
        uiState.selectedCastId = uiState.selectedCastId == castId ? nil : castId
        uiState.infoMessage = nil
    }

    private func clickDeleteCast() {
        guard uiState.selectedCastId != nil else {
            uiState.infoMessage = "삭제할 캐스트를 목록에서 선택해 주세요."
            return
        }
        uiState.isDeleteCastDialogVisible = true
        uiState.infoMessage = nil
    }

    private func dismissDeleteCastDialog() {
        uiState.isDeleteCastDialogVisible = false
    }

    private func confirmDeleteCast() {
        guard let selectedCastId = uiState.selectedCastId else {
            uiState.isDeleteCastDialogVisible = false
            uiState.infoMessage = "삭제할 캐스트를 목록에서 선택해 주세요."
            return
        }

        Task {
            do {
                let result = try await deleteCastUseCase.invoke(castId: selectedCastId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isDeleteCastDialogVisible = false
                    uiState.infoMessage = "캐스트 프로필을 삭제했습니다."
                } else if let failure = result as? AppResultFailure {
                    uiState.isDeleteCastDialogVisible = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.isDeleteCastDialogVisible = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func clickApproveCastClaim(_ claimId: String) {
        Task {
            do {
                let result = try await approveCastClaimUseCase.invoke(claimId: claimId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.infoMessage = "캐스트 프로필 연결 요청을 승인했습니다."
                    refreshClaimData(resetMessage: false)
                    refreshCastPreviews(resetMessage: false)
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func clickRejectCastClaim(_ claimId: String) {
        Task {
            do {
                let result = try await rejectCastClaimUseCase.invoke(claimId: claimId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.infoMessage = "캐스트 프로필 연결 요청을 반려했습니다."
                    refreshClaimData(resetMessage: false)
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func refreshClaimData(resetMessage: Bool = true) {
        if resetMessage {
            uiState.infoMessage = nil
        }
        loadPendingCastClaims()
    }

    private func loadPendingCastClaims() {
        Task {
            do {
                let result = try await getPendingCastClaimsForCafeUseCase.invoke(cafeId: cafeId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? [PendingCastClaimPreview] {
                    uiState.pendingCastClaims = data
                } else {
                    uiState.pendingCastClaims = []
                }
            } catch {
                uiState.pendingCastClaims = []
            }
        }
    }

    private func observeCafeDetailEvent() {
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let updated as CafeDetailEvent.CafeInfoUpdated:
                    if updated.cafeId == self.cafeId {
                        self.patchCafeInfo(updated.cafe)
                    }
                case is CafeDetailEvent.MenuCreated,
                     is CafeDetailEvent.MenuUpdated,
                     is CafeDetailEvent.MenuDeleted,
                     is CafeDetailEvent.GoodsCreated,
                     is CafeDetailEvent.GoodsUpdated,
                     is CafeDetailEvent.GoodsDeleted:
                    break
                default:
                    break
                }
            }
        }
    }

    private func observeBannerEvent() {
        watchHandles[.bannerEvent]?.cancel()
        watchHandles[.bannerEvent] = observeBannerEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let created = event as? BannerEvent.Created, created.banner.cafeId == self.cafeId {
                    self.loadCafeDashboard()
                }
            }
        }
    }

    private func patchCafeInfo(_ cafe: Cafe) {
        guard let current = uiState.cafe else { return }
        uiState.cafe = CafeDashboardData(
            id: current.id,
            name: cafe.name,
            city: cafe.region.city,
            todayCheckIns: current.todayCheckIns,
            todayReviews: current.todayReviews,
            rating: cafe.ratingAvg,
            castPreviews: current.castPreviews,
            homeBannerPreview: current.homeBannerPreview
        )
    }

    private func observeCastEvent() {
        watchHandles[.castEvent]?.cancel()
        watchHandles[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }

            Task { @MainActor in
                switch event {
                case let event as Shared.CastEvent.Created:
                    if event.cafeId == self.cafeId {
                        self.refreshCastPreviews()
                    }
                case let event as Shared.CastEvent.Updated:
                    if event.cafeId == self.cafeId {
                        self.uiState.castPreviews = self.uiState.castPreviews.map { preview in
                            guard preview.id == event.cast.id else { return preview }
                            return CafeCastPreview(
                                id: preview.id,
                                name: event.cast.name,
                                isOnShift: preview.isOnShift
                            )
                        }
                    }
                case let event as Shared.CastEvent.Deleted:
                    if event.cafeId == self.cafeId {
                        self.uiState.castPreviews.removeAll { $0.id == event.castId }
                        if self.uiState.selectedCastId == event.castId {
                            self.uiState.selectedCastId = nil
                        }
                        self.uiState.isDeleteCastDialogVisible = false
                        self.refreshClaimData(resetMessage: false)
                    }
                default:
                    break
                }
            }
        }
    }

    private func observeCastClaimEvent() {
        watchHandles[.castClaimEvent]?.cancel()
        watchHandles[.castClaimEvent] = observeCastClaimEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let created as Shared.CastClaimEvent.Created:
                    if created.claim.cafeId == self.cafeId {
                        self.refreshClaimData()
                    }
                case let updated as Shared.CastClaimEvent.Updated:
                    if updated.claim.cafeId == self.cafeId {
                        self.refreshClaimData()
                    }
                default:
                    break
                }
            }
        }
    }

    func onAction(_ action: CafeDashboardAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickShortcut(let shortcut):
            clickShortcut(shortcut)
        case .dismissExternalLinkSheet:
            dismissExternalLinkSheet()
        case .changeExternalLinkTitle(let value):
            changeExternalLinkTitle(value)
        case .changeExternalLinkUrl(let value):
            changeExternalLinkUrl(value)
        case .submitExternalLink:
            submitExternalLink()
        case .clickExternalLinkItem(let linkId):
            clickExternalLinkItem(linkId)
        case .clickDeleteExternalLink(let linkId):
            clickDeleteExternalLink(linkId)
        case .clickCastSchedule(let castId):
            clickCastSchedule(castId)
        case .clickDeleteCast:
            clickDeleteCast()
        case .confirmDeleteCast:
            confirmDeleteCast()
        case .dismissDeleteCastDialog:
            dismissDeleteCastDialog()
        case .clickApproveCastClaim(let claimId):
            clickApproveCastClaim(claimId)
        case .clickRejectCastClaim(let claimId):
            clickRejectCastClaim(claimId)
        case .clickLoadMoreCasts:
            clickLoadMoreCasts()
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }

    init(
        cafeId: String,
        getCafeCastPageUseCase: GetCafeCastPageUseCase = KoinInitializerKt.resolveGetCafeCastPageUseCase(),
        getCafeDashboardUseCase: GetCafeDashboardUseCase = KoinInitializerKt.resolveGetCafeDashboardUseCase(),
        getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase = KoinInitializerKt.resolveGetPendingCastClaimsForCafeUseCase(),
        approveCastClaimUseCase: ApproveCastClaimUseCase = KoinInitializerKt.resolveApproveCastClaimUseCase(),
        rejectCastClaimUseCase: RejectCastClaimUseCase = KoinInitializerKt.resolveRejectCastClaimUseCase(),
        deleteCastUseCase: DeleteCastUseCase = KoinInitializerKt.resolveDeleteCastUseCase(),
        observeBannerEventUseCase: ObserveBannerEventUseCase = KoinInitializerKt.resolveObserveBannerEventUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastClaimEventUseCase: ObserveCastClaimEventUseCase = KoinInitializerKt.resolveObserveCastClaimEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeCastPageUseCase = getCafeCastPageUseCase
        self.getCafeDashboardUseCase = getCafeDashboardUseCase
        self.getPendingCastClaimsForCafeUseCase = getPendingCastClaimsForCafeUseCase
        self.approveCastClaimUseCase = approveCastClaimUseCase
        self.rejectCastClaimUseCase = rejectCastClaimUseCase
        self.deleteCastUseCase = deleteCastUseCase
        self.observeBannerEventUseCase = observeBannerEventUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastClaimEventUseCase = observeCastClaimEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase

        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastClaimEvent()
        observeCastEvent()
        loadCafeDashboard()
    }

    deinit {
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case bannerEvent
        case cafeDetailEvent
        case castClaimEvent
        case castEvent
    }
}
