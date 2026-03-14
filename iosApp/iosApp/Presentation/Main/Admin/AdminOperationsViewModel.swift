//
//  AdminOperationsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
final class AdminOperationsViewModel: ObservableObject {
    @Published private(set) var uiState = AdminOperationsUiState()
    let event = PassthroughSubject<AdminOperationsEvent, Never>()

    private let getPendingCafeRegistrationClaimsUseCase: GetPendingCafeRegistrationClaimsUseCase

    private let getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase

    private let approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase

    private let approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase

    private let rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase

    private let rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase

    private func loadPendingRequests() {
        Task { @MainActor in
            do {
                let registration = try await getPendingCafeRegistrationClaimsUseCase.invoke()
                let roleClaimsResult = try await getPendingCafeOwnerClaimsUseCase.invoke()

                if let registrationSuccess = registration as? AppResultSuccess<AnyObject>,
                   let registrationClaims = registrationSuccess.data as? [PendingCafeRegistrationClaimPreview],
                   let roleClaimSuccess = roleClaimsResult as? AppResultSuccess<AnyObject>,
                   let roleClaims = roleClaimSuccess.data as? [PendingCafeOwnerClaimPreview] {
                    let mergedRequests = (
                        registrationClaims.map { Self.toAdminPendingRequest($0) } +
                        roleClaims.map { Self.toAdminPendingRequest($0) }
                    ).sorted { $0.requestedAt > $1.requestedAt }
                    uiState.pendingRequests = mergedRequests
                    uiState.metrics = buildAdminMetrics(pendingCount: mergedRequests.count)
                    uiState.infoMessage = nil
                } else if let failure = registration as? AppResultFailure {
                    uiState.pendingRequests = []
                    uiState.metrics = buildAdminMetrics(pendingCount: 0)
                    uiState.infoMessage = "\(failure.error)"
                } else if let failure = roleClaimsResult as? AppResultFailure {
                    uiState.pendingRequests = []
                    uiState.metrics = buildAdminMetrics(pendingCount: 0)
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.pendingRequests = []
                uiState.metrics = buildAdminMetrics(pendingCount: 0)
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func handlePendingResult(id: String, approved: Bool) {
        guard let request = uiState.pendingRequests.first(where: { $0.id == id }) else { return }

        Task { @MainActor in
            do {
                let result: AppResult
                switch request.type {
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
                    loadPendingRequests()
                    uiState.infoMessage = approved
                        ? "\(request.title) 요청을 승인했습니다."
                        : "\(request.title) 요청을 반려했습니다."
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
                event.send(.navigateToBannerEdit)
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
        approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase = KoinInitializerKt.resolveApproveCafeRegistrationClaimUseCase(),
        approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase = KoinInitializerKt.resolveApproveCafeOwnerClaimUseCase(),
        rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase = KoinInitializerKt.resolveRejectCafeRegistrationClaimUseCase(),
        rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase = KoinInitializerKt.resolveRejectCafeOwnerClaimUseCase()
    ) {
        self.getPendingCafeRegistrationClaimsUseCase = getPendingCafeRegistrationClaimsUseCase
        self.getPendingCafeOwnerClaimsUseCase = getPendingCafeOwnerClaimsUseCase
        self.approveCafeRegistrationClaimUseCase = approveCafeRegistrationClaimUseCase
        self.approveCafeOwnerClaimUseCase = approveCafeOwnerClaimUseCase
        self.rejectCafeRegistrationClaimUseCase = rejectCafeRegistrationClaimUseCase
        self.rejectCafeOwnerClaimUseCase = rejectCafeOwnerClaimUseCase
        loadPendingRequests()
    }
}

private let adminBannerMenuId = "banner"

private extension AdminOperationsViewModel {
    static func toAdminPendingRequest(_ claim: PendingCafeOwnerClaimPreview) -> AdminPendingRequest {
        AdminPendingRequest(
            id: claim.claimId,
            type: .roleClaim,
            title: "점장 권한 신청 - \(claim.requesterNickname)",
            subtitle: claim.location,
            requestedAt: claim.requestedAt,
            imageUrl: claim.imageUrl ?? ""
        )
    }

    static func toAdminPendingRequest(_ claim: PendingCafeRegistrationClaimPreview) -> AdminPendingRequest {
        AdminPendingRequest(
            id: claim.claimId,
            type: .cafeRegistration,
            title: claim.cafeName,
            subtitle: claim.location,
            requestedAt: claim.requestedAt,
            imageUrl: claim.imageUrl ?? ""
        )
    }
}
