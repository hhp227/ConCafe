//
//  AdminOperationsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class AdminOperationsViewModel: ObservableObject {
    @Published private(set) var uiState = AdminOperationsUiState()

    let event = PassthroughSubject<AdminOperationsEvent, Never>()

    private let getPendingCafeRegistrationClaimsUseCase: GetPendingCafeRegistrationClaimsUseCase

    private let getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase

    private let getAdminOperationsMetricsUseCase: GetAdminOperationsMetricsUseCase

    private let approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase

    private let approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase

    private let rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase

    private let rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase

    private let cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadPendingRequests() {
        Task { @MainActor in
            do {
                let registration = try await getPendingCafeRegistrationClaimsUseCase.invoke()
                let roleClaimsResult = try await getPendingCafeOwnerClaimsUseCase.invoke()
                let adminMetricsResult = try await getAdminOperationsMetricsUseCase.invoke()

                if let registrationSuccess = registration as? AppResultSuccess<AnyObject>,
                   let registrationClaims = registrationSuccess.data as? [PendingCafeRegistrationClaimPreview],
                   let roleClaimSuccess = roleClaimsResult as? AppResultSuccess<AnyObject>,
                   let roleClaims = roleClaimSuccess.data as? [PendingCafeOwnerClaimPreview],
                   let metricsSuccess = adminMetricsResult as? AppResultSuccess<AnyObject>,
                   let metrics = metricsSuccess.data as? AdminOperationsMetrics {
                    let sortedRegistrations = registrationClaims.sorted { $0.requestedAt > $1.requestedAt }
                    let sortedOwnerClaims = roleClaims.sorted { $0.requestedAt > $1.requestedAt }
                    let pendingCount = sortedRegistrations.count + sortedOwnerClaims.count
                    let totalUsersCount = Int(metrics.totalUsersCount)
                    let activeCafesCount = Int(metrics.activeCafesCount)
                    let reportItemsCount = Int(metrics.reportItemsCount)

                    uiState.totalUsersCount = totalUsersCount
                    uiState.activeCafesCount = activeCafesCount
                    uiState.reportItemsCount = reportItemsCount
                    uiState.pendingCafeRegistrationClaims = sortedRegistrations
                    uiState.pendingCafeOwnerClaims = sortedOwnerClaims
                    uiState.metrics = buildAdminMetrics(
                        totalUsersCount: totalUsersCount,
                        activeCafesCount: activeCafesCount,
                        pendingCount: pendingCount,
                        reportItemsCount: reportItemsCount
                    )
                    uiState.infoMessage = nil
                    AdminPendingCache.snapshot = AdminPendingSnapshot(
                        registrationClaims: sortedRegistrations,
                        ownerClaims: sortedOwnerClaims,
                        totalUsersCount: totalUsersCount,
                        activeCafesCount: activeCafesCount,
                        reportItemsCount: reportItemsCount
                    )
                } else if let failure = registration as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                } else if let failure = roleClaimsResult as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                } else if let failure = adminMetricsResult as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func showCachedPendingRequests() {
        guard let snapshot = AdminPendingCache.snapshot else { return }
        let pendingCount = snapshot.registrationClaims.count + snapshot.ownerClaims.count
        uiState.totalUsersCount = snapshot.totalUsersCount
        uiState.activeCafesCount = snapshot.activeCafesCount
        uiState.reportItemsCount = snapshot.reportItemsCount
        uiState.pendingCafeRegistrationClaims = snapshot.registrationClaims
        uiState.pendingCafeOwnerClaims = snapshot.ownerClaims
        uiState.metrics = buildAdminMetrics(
            totalUsersCount: snapshot.totalUsersCount,
            activeCafesCount: snapshot.activeCafesCount,
            pendingCount: pendingCount,
            reportItemsCount: snapshot.reportItemsCount
        )
    }

    private func observeClaimEvents() {
        tasks[.claimEvent]?.cancel()
        tasks[.claimEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeRegistrationClaimEventPublisher.events) {
                    switch event {
                    case let created as CafeRegistrationClaimEvent.Created:
                        _ = created
                        self.loadPendingRequests()
                    case let approved as CafeRegistrationClaimEvent.Approved:
                        self.removeRegistrationClaimLocally(claimId: approved.claimId)
                    case let rejected as CafeRegistrationClaimEvent.Rejected:
                        self.removeRegistrationClaimLocally(claimId: rejected.claimId)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func removeRegistrationClaimLocally(claimId: String) {
        let totalUsersCount = uiState.totalUsersCount
        let activeCafesCount = uiState.activeCafesCount
        let reportItemsCount = uiState.reportItemsCount
        uiState.pendingCafeRegistrationClaims.removeAll { $0.claimId == claimId }
        uiState.metrics = buildAdminMetrics(
            totalUsersCount: totalUsersCount,
            activeCafesCount: activeCafesCount,
            pendingCount: uiState.pendingCafeRegistrationClaims.count + uiState.pendingCafeOwnerClaims.count,
            reportItemsCount: reportItemsCount
        )
        AdminPendingCache.snapshot = AdminPendingSnapshot(
            registrationClaims: uiState.pendingCafeRegistrationClaims,
            ownerClaims: uiState.pendingCafeOwnerClaims,
            totalUsersCount: totalUsersCount,
            activeCafesCount: activeCafesCount,
            reportItemsCount: reportItemsCount
        )
    }

    private func removeOwnerClaimLocally(claimId: String) {
        let totalUsersCount = uiState.totalUsersCount
        let activeCafesCount = uiState.activeCafesCount
        let reportItemsCount = uiState.reportItemsCount
        uiState.pendingCafeOwnerClaims.removeAll { $0.claimId == claimId }
        uiState.metrics = buildAdminMetrics(
            totalUsersCount: totalUsersCount,
            activeCafesCount: activeCafesCount,
            pendingCount: uiState.pendingCafeRegistrationClaims.count + uiState.pendingCafeOwnerClaims.count,
            reportItemsCount: reportItemsCount
        )
        AdminPendingCache.snapshot = AdminPendingSnapshot(
            registrationClaims: uiState.pendingCafeRegistrationClaims,
            ownerClaims: uiState.pendingCafeOwnerClaims,
            totalUsersCount: totalUsersCount,
            activeCafesCount: activeCafesCount,
            reportItemsCount: reportItemsCount
        )
    }

    private func handlePendingResult(id: String, approved: Bool) {
        let selectedFilter = uiState.selectedPendingFilter
        let requestTitle: String
        if selectedFilter == .cafeRegistration {
            guard let claim = uiState.pendingCafeRegistrationClaims.first(where: { $0.claimId == id }) else { return }
            requestTitle = claim.cafeName
        } else {
            guard let claim = uiState.pendingCafeOwnerClaims.first(where: { $0.claimId == id }) else { return }
            requestTitle = "점장 권한 신청 - \(claim.requesterNickname)"
        }

        Task { @MainActor in
            do {
                let result: AppResult
                switch selectedFilter {
                case .cafeRegistration:
                    result = approved
                        ? try await approveCafeRegistrationClaimUseCase.invoke(claimId: id)
                        : try await rejectCafeRegistrationClaimUseCase.invoke(claimId: id)
                case .roleClaim:
                    result = approved
                        ? try await approveCafeOwnerClaimUseCase.invoke(claimId: id)
                        : try await rejectCafeOwnerClaimUseCase.invoke(claimId: id)
                }

                if result is AppResultSuccess<AnyObject> {
                    if selectedFilter == .cafeRegistration {
                        removeRegistrationClaimLocally(claimId: id)
                    } else {
                        removeOwnerClaimLocally(claimId: id)
                    }
                    uiState.infoMessage = approved
                        ? "\(requestTitle) 요청을 승인했습니다."
                        : "\(requestTitle) 요청을 반려했습니다."
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    func onAction(_ action: AdminOperationsAction) {
        switch action {
        case .clickNotifications:
            uiState.hasUnreadNotifications = false
            uiState.infoMessage = "새 알림을 모두 확인했습니다."
        case .clickSeeAllPending:
            uiState.infoMessage = "전체보기 연결은 다음 단계에서 이어집니다."
        case .clickBannerRegister:
            event.send(.navigateToBannerEdit)
        case .selectPendingFilter(let filter):
            uiState.selectedPendingFilter = filter
            uiState.infoMessage = nil
        case .approvePending(let id):
            handlePendingResult(id: id, approved: true)
        case .rejectPending(let id):
            handlePendingResult(id: id, approved: false)
        case .clickQuickMenu(let id):
            if id == adminBannerMenuId {
                event.send(.navigateToBanner)
            } else {
                let label = uiState.quickMenus.first(where: { $0.id == id })?.title ?? "메뉴"
                uiState.infoMessage = "\(label) 연결은 다음 단계에서 이어집니다."
            }
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        getPendingCafeRegistrationClaimsUseCase: GetPendingCafeRegistrationClaimsUseCase = KoinInitializerKt.resolveGetPendingCafeRegistrationClaimsUseCase(),
        getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase = KoinInitializerKt.resolveGetPendingCafeOwnerClaimsUseCase(),
        getAdminOperationsMetricsUseCase: GetAdminOperationsMetricsUseCase = KoinInitializerKt.resolveGetAdminOperationsMetricsUseCase(),
        approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase = KoinInitializerKt.resolveApproveCafeRegistrationClaimUseCase(),
        approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase = KoinInitializerKt.resolveApproveCafeOwnerClaimUseCase(),
        rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase = KoinInitializerKt.resolveRejectCafeRegistrationClaimUseCase(),
        rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase = KoinInitializerKt.resolveRejectCafeOwnerClaimUseCase(),
        cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher = KoinInitializerKt.resolveCafeRegistrationClaimEventPublisher()
    ) {
        self.getPendingCafeRegistrationClaimsUseCase = getPendingCafeRegistrationClaimsUseCase
        self.getPendingCafeOwnerClaimsUseCase = getPendingCafeOwnerClaimsUseCase
        self.getAdminOperationsMetricsUseCase = getAdminOperationsMetricsUseCase
        self.approveCafeRegistrationClaimUseCase = approveCafeRegistrationClaimUseCase
        self.approveCafeOwnerClaimUseCase = approveCafeOwnerClaimUseCase
        self.rejectCafeRegistrationClaimUseCase = rejectCafeRegistrationClaimUseCase
        self.rejectCafeOwnerClaimUseCase = rejectCafeOwnerClaimUseCase
        self.cafeRegistrationClaimEventPublisher = cafeRegistrationClaimEventPublisher
        showCachedPendingRequests()
        observeClaimEvents()
        loadPendingRequests()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case claimEvent
    }
}

private let adminBannerMenuId = "banner"

private struct AdminPendingSnapshot {
    let registrationClaims: [PendingCafeRegistrationClaimPreview]
    let ownerClaims: [PendingCafeOwnerClaimPreview]
    let totalUsersCount: Int
    let activeCafesCount: Int
    let reportItemsCount: Int
}

private enum AdminPendingCache {
    static var snapshot: AdminPendingSnapshot?
}
