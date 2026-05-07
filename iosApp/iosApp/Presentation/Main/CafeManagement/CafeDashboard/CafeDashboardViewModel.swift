//
//  CafeDashboardViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CafeDashboardViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeCastPageUseCase: GetCafeCastPageUseCase

    private let getCafeDashboardUseCase: GetCafeDashboardUseCase

    private let getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase

    private let approveCastClaimUseCase: ApproveCastClaimUseCase

    private let rejectCastClaimUseCase: RejectCastClaimUseCase

    private let cafeExternalLinkLocalUseCase: CafeExternalLinkLocalUseCase

    private let updateCafeSocialMediaUseCase: UpdateCafeSocialMediaUseCase

    private let updateCafeReservationUrlUseCase: UpdateCafeReservationUrlUseCase

    private let updateCafeTableCountsUseCase: UpdateCafeTableCountsUseCase

    private let deleteCastUseCase: DeleteCastUseCase

    private let bannerEventPublisher: BannerEventPublisher

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castClaimEventPublisher: CastClaimEventPublisher

    private let castEventPublisher: CastEventPublisher

    private let visitEventPublisher: VisitEventPublisher

    @Published private(set) var uiState = CafeDashboardUiState()

    let event = PassthroughSubject<CafeDashboardEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadCafeDashboard() {
        Task {
            uiState.isLoading = true
            uiState.infoMessage = nil

            do {
                let result = try await getCafeDashboardUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeDashboardData {
                    uiState.cafe = data
                    let socialMedia = (data.socialMedia as? [String: String]) ?? [:]
                    uiState.instagramId = socialMedia["instagram"] ?? ""
                    uiState.twitterId = socialMedia["twitter"] ?? ""
                    uiState.tiktokId = socialMedia["tiktok"] ?? ""
                    uiState.youtubeId = socialMedia["youtube"] ?? ""
                    uiState.reservationUrl = data.reservationUrl ?? ""
                    uiState.currentTableCountInput = String(data.tableCounts.current)
                    uiState.totalTableCountInput = String(data.tableCounts.total)
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
                if append {
                    try await Task.sleep(nanoseconds: Self.paginationDelayNanoseconds)
                    if Task.isCancelled { return }
                }
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
                    uiState.infoMessage = "dashboard_info_cast_list_load_failed"
                }
            } catch {
                uiState.isLoadingMoreCasts = false
                uiState.infoMessage = "dashboard_info_cast_list_load_failed"
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
            event.send(.navigateToBanner)
        case .eventManagement:
            event.send(.navigateToNoticeEvent(cafeId: cafeId))
        case .menuGoods:
            event.send(.navigateToMenuGoods(cafeId: cafeId))
        case .castSchedule:
            guard let selectedCastId = uiState.selectedCastId else {
                uiState.infoMessage = "dashboard_info_select_cast_for_schedule"
                return
            }
            event.send(.navigateToSchedule(castId: selectedCastId))
        case .castManagement:
            event.send(.navigateToCastEdit(cafeId: cafeId, castId: nil))
        case .externalLinks:
            uiState.isExternalLinkSheetVisible = true
            uiState.infoMessage = nil
        case .socialMedia:
            uiState.isSocialMediaSheetVisible = true
            uiState.infoMessage = nil
        case .reservation:
            uiState.isReservationSheetVisible = true
            uiState.infoMessage = nil
        }
    }

    private func clickCreateBanner() {
        event.send(.navigateToBannerEdit)
    }

    private func loadExternalLinks() {
        let links = cafeExternalLinkLocalUseCase.load(cafeId: cafeId).map { persisted in
            CafeDashboardExternalLink(
                id: persisted.id,
                title: persisted.title,
                url: persisted.url
            )
        }
        uiState.externalLinks = links
    }

    private func dismissExternalLinkSheet() {
        uiState.isExternalLinkSheetVisible = false
        uiState.editingExternalLinkId = nil
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
            uiState.infoMessage = "dashboard_info_external_link_input_required"
            return
        }

        let isEdit = uiState.editingExternalLinkId != nil
        let title = uiState.externalLinkTitle
        let url = uiState.externalLinkUrl
        let persisted = cafeExternalLinkLocalUseCase.upsert(
            cafeId: cafeId,
            linkId: uiState.editingExternalLinkId,
            title: title,
            url: url
        )

        uiState.externalLinks = persisted.map { item in
            CafeDashboardExternalLink(
                id: item.id,
                title: item.title,
                url: item.url
            )
        }
        uiState.isExternalLinkSheetVisible = false
        uiState.editingExternalLinkId = nil
        uiState.externalLinkTitle = ""
        uiState.externalLinkUrl = ""
        uiState.infoMessage = isEdit ? "dashboard_info_external_link_updated" : "dashboard_info_external_link_added"
    }

    private func clickExternalLinkItem(_ linkId: String) {
        guard let link = uiState.externalLinks.first(where: { $0.id == linkId }) else { return }
        event.send(.navigateToExternalLink(title: link.title, url: link.url))
    }

    private func clickEditExternalLink(_ linkId: String) {
        guard let link = uiState.externalLinks.first(where: { $0.id == linkId }) else { return }
        uiState.isExternalLinkSheetVisible = true
        uiState.editingExternalLinkId = link.id
        uiState.externalLinkTitle = link.title
        uiState.externalLinkUrl = link.url
        uiState.infoMessage = nil
    }

    private func clickDeleteExternalLink(_ linkId: String) {
        let persisted = cafeExternalLinkLocalUseCase.delete(cafeId: cafeId, linkId: linkId)
        uiState.externalLinks = persisted.map { item in
            CafeDashboardExternalLink(
                id: item.id,
                title: item.title,
                url: item.url
            )
        }
        uiState.infoMessage = "dashboard_info_external_link_deleted"
    }

    private func changeSocialMediaInstagram(_ value: String) {
        uiState.instagramId = value
    }

    private func changeSocialMediaTwitter(_ value: String) {
        uiState.twitterId = value
    }

    private func changeSocialMediaTiktok(_ value: String) {
        uiState.tiktokId = value
    }

    private func changeSocialMediaYoutube(_ value: String) {
        uiState.youtubeId = value
    }

    private func submitSocialMedia() {
        let instagramId = uiState.instagramId.trimmingCharacters(in: .whitespacesAndNewlines)
        let twitterId = uiState.twitterId.trimmingCharacters(in: .whitespacesAndNewlines)
        let tiktokId = uiState.tiktokId.trimmingCharacters(in: .whitespacesAndNewlines)
        let youtubeId = uiState.youtubeId.trimmingCharacters(in: .whitespacesAndNewlines)
        uiState.isSavingSocialMedia = true
        Task {
            do {
                let result = try await updateCafeSocialMediaUseCase.invoke(
                    cafeId: cafeId,
                    instagramId: instagramId.isEmpty ? nil : instagramId,
                    twitterId: twitterId.isEmpty ? nil : twitterId,
                    tiktokId: tiktokId.isEmpty ? nil : tiktokId,
                    youtubeId: youtubeId.isEmpty ? nil : youtubeId
                )
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSavingSocialMedia = false
                    uiState.isSocialMediaSheetVisible = false
                    uiState.infoMessage = "dashboard_info_social_media_saved"
                } else if let failure = result as? AppResultFailure {
                    uiState.isSavingSocialMedia = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.isSavingSocialMedia = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func dismissSocialMediaSheet() {
        uiState.isSocialMediaSheetVisible = false
    }

    private func dismissReservationSheet() {
        uiState.isReservationSheetVisible = false
        uiState.reservationUrl = uiState.cafe?.reservationUrl ?? ""
    }

    private func clickTableCountMetric() {
        uiState.isTableCountSheetVisible = true
        uiState.infoMessage = nil
    }

    private func dismissTableCountSheet() {
        uiState.isTableCountSheetVisible = false
        let tableCounts = uiState.cafe?.tableCounts
        uiState.currentTableCountInput = String(tableCounts?.current ?? 0)
        uiState.totalTableCountInput = String(tableCounts?.total ?? 0)
    }

    private func changeCurrentTableCount(_ value: String) {
        uiState.currentTableCountInput = value
    }

    private func changeTotalTableCount(_ value: String) {
        uiState.totalTableCountInput = value
        if let newTotal = Int32(value), let current = Int32(uiState.currentTableCountInput), current > newTotal {
            uiState.currentTableCountInput = value
        }
    }

    private func submitTableCounts() {
        guard uiState.isTableCountSubmitEnabled else { return }
        guard let current = Int32(uiState.currentTableCountInput),
              let total = Int32(uiState.totalTableCountInput) else { return }
        uiState.isSavingTableCounts = true
        Task {
            do {
                let result = try await updateCafeTableCountsUseCase.invoke(
                    cafeId: cafeId,
                    current: current,
                    total: total
                )
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSavingTableCounts = false
                    uiState.isTableCountSheetVisible = false
                    uiState.infoMessage = "dashboard_info_table_counts_saved"
                    if let cafeCurrent = uiState.cafe {
                        uiState.cafe = CafeDashboardData(
                            id: cafeCurrent.id,
                            name: cafeCurrent.name,
                            city: cafeCurrent.city,
                            todayCheckIns: cafeCurrent.todayCheckIns,
                            followerCount: cafeCurrent.followerCount,
                            rating: cafeCurrent.rating,
                            castPreviews: cafeCurrent.castPreviews,
                            homeBannerPreview: cafeCurrent.homeBannerPreview,
                            socialMedia: cafeCurrent.socialMedia,
                            reservationUrl: cafeCurrent.reservationUrl,
                            tableCounts: TableCounts(current: current, total: total)
                        )
                    }
                } else if let failure = result as? AppResultFailure {
                    uiState.isSavingTableCounts = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.isSavingTableCounts = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func changeReservationUrl(_ value: String) {
        uiState.reservationUrl = value
    }

    private func submitReservation() {
        let url = uiState.reservationUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        uiState.isSavingReservation = true
        Task {
            do {
                let result = try await updateCafeReservationUrlUseCase.invoke(
                    cafeId: cafeId,
                    reservationUrl: url.isEmpty ? nil : url
                )
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSavingReservation = false
                    uiState.isReservationSheetVisible = false
                    uiState.infoMessage = "dashboard_info_reservation_saved"
                    if let current = uiState.cafe {
                        uiState.cafe = CafeDashboardData(
                            id: current.id,
                            name: current.name,
                            city: current.city,
                            todayCheckIns: current.todayCheckIns,
                            followerCount: current.followerCount,
                            rating: current.rating,
                            castPreviews: current.castPreviews,
                            homeBannerPreview: current.homeBannerPreview,
                            socialMedia: current.socialMedia,
                            reservationUrl: url.isEmpty ? nil : url,
                            tableCounts: current.tableCounts
                        )
                    }
                } else if let failure = result as? AppResultFailure {
                    uiState.isSavingReservation = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.isSavingReservation = false
                uiState.infoMessage = error.localizedDescription
            }
        }
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
            uiState.infoMessage = "dashboard_info_select_cast_for_delete"
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
            uiState.infoMessage = "dashboard_info_select_cast_for_delete"
            return
        }

        Task {
            do {
                let result = try await deleteCastUseCase.invoke(castId: selectedCastId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isDeleteCastDialogVisible = false
                    uiState.infoMessage = "dashboard_info_cast_deleted"
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
                    uiState.infoMessage = "dashboard_info_cast_claim_approved"
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
                    uiState.infoMessage = "dashboard_info_cast_claim_rejected"
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
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    switch event {
                    case let updated as CafeDetailEvent.CafeInfoUpdated:
                        if updated.cafeId == self.cafeId {
                            self.patchCafeInfo(updated.cafe)
                        }
                    case let favorite as CafeDetailEvent.FavoriteToggled:
                        if favorite.cafeId == self.cafeId {
                            self.patchFollowerCount(isFavorite: favorite.isFavorite)
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
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeBannerEvent() {
        tasks[.bannerEvent]?.cancel()
        tasks[.bannerEvent] = Task {
            do {
                for try await event in asyncSequence(for: bannerEventPublisher.events) {
                    if let created = event as? Shared.BannerEvent.Created, created.banner.cafeId == self.cafeId {
                        self.loadCafeDashboard()
                    } else if let updated = event as? Shared.BannerEvent.Updated, updated.banner.cafeId == self.cafeId {
                        self.patchUpdatedBannerPreview(updated.banner)
                    } else if let deleted = event as? Shared.BannerEvent.Deleted, deleted.banner.cafeId == self.cafeId {
                        self.loadCafeDashboard()
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchUpdatedBannerPreview(_ updatedBanner: HomeBanner) {
        if let current = uiState.cafe {
            let currentPreview = current.homeBannerPreview
            let statusLabel: String
            switch updatedBanner.statusLabel.uppercased() {
            case "ACTIVE":
                statusLabel = "dashboard_banner_status_active"
            case "SCHEDULED":
                statusLabel = "dashboard_banner_status_scheduled"
            default:
                statusLabel = "dashboard_banner_status_hidden"
            }
            uiState.cafe = CafeDashboardData(
                id: current.id,
                name: current.name,
                city: current.city,
                todayCheckIns: current.todayCheckIns,
                followerCount: current.followerCount,
                rating: current.rating,
                castPreviews: current.castPreviews,
                homeBannerPreview: CafeDashboardData.HomeBannerPreview(
                    title: updatedBanner.title,
                    period: "dashboard_banner_period_days:\(updatedBanner.displayDays)",
                    statusLabel: statusLabel,
                    imageUrl: updatedBanner.imageUrl
                ),
                socialMedia: current.socialMedia,
                reservationUrl: current.reservationUrl,
                tableCounts: current.tableCounts
            )
        }
    }

    private func patchCafeInfo(_ cafe: Cafe) {
        guard let current = uiState.cafe else { return }
        uiState.cafe = CafeDashboardData(
            id: current.id,
            name: cafe.name,
            city: cafe.region.city,
            todayCheckIns: current.todayCheckIns,
            followerCount: current.followerCount,
            rating: cafe.ratingAvg,
            castPreviews: current.castPreviews,
            homeBannerPreview: current.homeBannerPreview,
            socialMedia: current.socialMedia,
            reservationUrl: current.reservationUrl,
            tableCounts: current.tableCounts
        )
    }

    private func patchFollowerCount(isFavorite: Bool) {
        guard let current = uiState.cafe else { return }
        let nextCount = isFavorite
            ? Int(current.followerCount) + 1
            : max(Int(current.followerCount) - 1, 0)
        uiState.cafe = CafeDashboardData(
            id: current.id,
            name: current.name,
            city: current.city,
            todayCheckIns: current.todayCheckIns,
            followerCount: Int32(nextCount),
            rating: current.rating,
            castPreviews: current.castPreviews,
            homeBannerPreview: current.homeBannerPreview,
            socialMedia: current.socialMedia,
            reservationUrl: current.reservationUrl,
            tableCounts: current.tableCounts
        )
    }

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
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
                                    isOnShift: preview.isOnShift,
                                    profileImage: event.cast.profileImage
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
                    if let created = event as? Shared.VisitEvent.Created, created.cafeId == self.cafeId {
                        if let current = self.uiState.cafe {
                            self.uiState.cafe = CafeDashboardData(
                                id: current.id,
                                name: current.name,
                                city: current.city,
                                todayCheckIns: current.todayCheckIns + 1,
                                followerCount: current.followerCount,
                                rating: current.rating,
                                castPreviews: current.castPreviews,
                                homeBannerPreview: current.homeBannerPreview,
                                socialMedia: current.socialMedia,
                                reservationUrl: current.reservationUrl,
                                tableCounts: current.tableCounts
                            )
                        }
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCastClaimEvent() {
        tasks[.castClaimEvent]?.cancel()
        tasks[.castClaimEvent] = Task {
            do {
                for try await event in asyncSequence(for: castClaimEventPublisher.events) {
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
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func startCastClaimPolling() {
        let pollingIntervalNanoseconds = castClaimPollingIntervalNanoseconds
        tasks[.castClaimPolling]?.cancel()
        tasks[.castClaimPolling] = Task { [weak self] in
            while !Task.isCancelled {
                do {
                    try await Task.sleep(nanoseconds: pollingIntervalNanoseconds)
                } catch {
                    break
                }

                if Task.isCancelled {
                    break
                } else if let self {
                    await self.refreshClaimData(resetMessage: false)
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
        case .clickCreateBanner:
            clickCreateBanner()
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
        case .clickEditExternalLink(let linkId):
            clickEditExternalLink(linkId)
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
        case .changeSocialMediaInstagram(let value):
            changeSocialMediaInstagram(value)
        case .changeSocialMediaTwitter(let value):
            changeSocialMediaTwitter(value)
        case .changeSocialMediaTiktok(let value):
            changeSocialMediaTiktok(value)
        case .changeSocialMediaYoutube(let value):
            changeSocialMediaYoutube(value)
        case .submitSocialMedia:
            submitSocialMedia()
        case .dismissSocialMediaSheet:
            dismissSocialMediaSheet()
        case .dismissReservationSheet:
            dismissReservationSheet()
        case .changeReservationUrl(let value):
            changeReservationUrl(value)
        case .submitReservation:
            submitReservation()
        case .clickTableCountMetric:
            clickTableCountMetric()
        case .dismissTableCountSheet:
            dismissTableCountSheet()
        case .changeCurrentTableCount(let value):
            changeCurrentTableCount(value)
        case .changeTotalTableCount(let value):
            changeTotalTableCount(value)
        case .submitTableCounts:
            submitTableCounts()
        }
    }

    init(
        cafeId: String,
        getCafeCastPageUseCase: GetCafeCastPageUseCase = KoinInitializerKt.resolveGetCafeCastPageUseCase(),
        getCafeDashboardUseCase: GetCafeDashboardUseCase = KoinInitializerKt.resolveGetCafeDashboardUseCase(),
        getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase = KoinInitializerKt.resolveGetPendingCastClaimsForCafeUseCase(),
        approveCastClaimUseCase: ApproveCastClaimUseCase = KoinInitializerKt.resolveApproveCastClaimUseCase(),
        rejectCastClaimUseCase: RejectCastClaimUseCase = KoinInitializerKt.resolveRejectCastClaimUseCase(),
        cafeExternalLinkLocalUseCase: CafeExternalLinkLocalUseCase = KoinInitializerKt.resolveCafeExternalLinkLocalUseCase(),
        updateCafeSocialMediaUseCase: UpdateCafeSocialMediaUseCase = KoinInitializerKt.resolveUpdateCafeSocialMediaUseCase(),
        updateCafeReservationUrlUseCase: UpdateCafeReservationUrlUseCase = KoinInitializerKt.resolveUpdateCafeReservationUrlUseCase(),
        updateCafeTableCountsUseCase: UpdateCafeTableCountsUseCase = KoinInitializerKt.resolveUpdateCafeTableCountsUseCase(),
        deleteCastUseCase: DeleteCastUseCase = KoinInitializerKt.resolveDeleteCastUseCase(),
        bannerEventPublisher: BannerEventPublisher = KoinInitializerKt.resolveBannerEventPublisher(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castClaimEventPublisher: CastClaimEventPublisher = KoinInitializerKt.resolveCastClaimEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        visitEventPublisher: VisitEventPublisher = KoinInitializerKt.resolveVisitEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getCafeCastPageUseCase = getCafeCastPageUseCase
        self.getCafeDashboardUseCase = getCafeDashboardUseCase
        self.getPendingCastClaimsForCafeUseCase = getPendingCastClaimsForCafeUseCase
        self.approveCastClaimUseCase = approveCastClaimUseCase
        self.rejectCastClaimUseCase = rejectCastClaimUseCase
        self.cafeExternalLinkLocalUseCase = cafeExternalLinkLocalUseCase
        self.updateCafeSocialMediaUseCase = updateCafeSocialMediaUseCase
        self.updateCafeReservationUrlUseCase = updateCafeReservationUrlUseCase
        self.updateCafeTableCountsUseCase = updateCafeTableCountsUseCase
        self.deleteCastUseCase = deleteCastUseCase
        self.bannerEventPublisher = bannerEventPublisher
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castClaimEventPublisher = castClaimEventPublisher
        self.castEventPublisher = castEventPublisher
        self.visitEventPublisher = visitEventPublisher

        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastClaimEvent()
        observeCastEvent()
        observeVisitEvent()
        startCastClaimPolling()
        loadExternalLinks()
        loadCafeDashboard()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case bannerEvent
        case cafeDetailEvent
        case castClaimEvent
        case castEvent
        case visitEvent
        case castClaimPolling
    }

    private let castClaimPollingIntervalNanoseconds: UInt64 = 30_000_000_000
    private static let paginationDelayNanoseconds: UInt64 = 1_000_000_000
}
