//
//  AdminOperationsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

@MainActor
final class AdminOperationsViewModel: ObservableObject {
    @Published private(set) var uiState = AdminOperationsUiState()

    private let getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase

    private let approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase

    private let rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase

    private func loadPendingRequests() {
        Task {
            do {
                let result = try await getPendingCafeOwnerClaimsUseCase.invoke()
                if let success = result as? AppResultSuccess<AnyObject>,
                   let claims = success.data as? [PendingCafeOwnerClaimPreview] {
                    let roleClaims = claims.map { Self.toAdminPendingRequest($0) }
                    let mergedRequests = defaultCafeRegistrationPendingRequests + roleClaims
                    uiState.pendingRequests = mergedRequests
                    uiState.metrics = Self.buildMetrics(pendingCount: mergedRequests.count)
                    uiState.infoMessage = nil
                } else if let failure = result as? AppResultFailure {
                    uiState.pendingRequests = defaultCafeRegistrationPendingRequests
                    uiState.metrics = Self.buildMetrics(pendingCount: defaultCafeRegistrationPendingRequests.count)
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.pendingRequests = defaultCafeRegistrationPendingRequests
                uiState.metrics = Self.buildMetrics(pendingCount: defaultCafeRegistrationPendingRequests.count)
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func handlePendingResult(id: String, approved: Bool) {
        guard let request = uiState.pendingRequests.first(where: { $0.id == id }) else { return }
        if request.type == .roleClaim {
            Task {
                do {
                    let result = approved
                        ? try await approveCafeOwnerClaimUseCase.invoke(claimId: id)
                        : try await rejectCafeOwnerClaimUseCase.invoke(claimId: id)
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
            return
        }

        uiState.pendingRequests.removeAll { $0.id == id }
        uiState.metrics = Self.buildMetrics(pendingCount: uiState.pendingRequests.count)
        uiState.infoMessage = approved
        ? "\(request.title) 요청을 승인했습니다."
        : "\(request.title) 요청을 반려했습니다."
    }

    func onAction(_ action: AdminOperationsAction) {
        switch action {
        case .clickNotifications:
            uiState.hasUnreadNotifications = false
            uiState.infoMessage = "새 알림을 모두 확인했습니다."
        case .clickSeeAllPending:
            uiState.infoMessage = "전체보기 연결은 다음 단계에서 이어집니다."
        case .selectPendingFilter(let filter):
            uiState.selectedPendingFilter = filter
            uiState.infoMessage = nil
        case .approvePending(let id):
            handlePendingResult(id: id, approved: true)
        case .rejectPending(let id):
            handlePendingResult(id: id, approved: false)
        case .clickQuickMenu(let id):
            let label = uiState.quickMenus.first(where: { $0.id == id })?.title ?? "메뉴"
            uiState.infoMessage = "\(label) 연결은 다음 단계에서 이어집니다."
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase = KoinInitializerKt.resolveGetPendingCafeOwnerClaimsUseCase(),
        approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase = KoinInitializerKt.resolveApproveCafeOwnerClaimUseCase(),
        rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase = KoinInitializerKt.resolveRejectCafeOwnerClaimUseCase()
    ) {
        self.getPendingCafeOwnerClaimsUseCase = getPendingCafeOwnerClaimsUseCase
        self.approveCafeOwnerClaimUseCase = approveCafeOwnerClaimUseCase
        self.rejectCafeOwnerClaimUseCase = rejectCafeOwnerClaimUseCase
        loadPendingRequests()
    }
}

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

    static func buildMetrics(pendingCount: Int) -> [AdminMetricCard] {
        [
            AdminMetricCard(title: "전체 사용자", value: "12,540", delta: "1.2%", icon: .users, trend: .up),
            AdminMetricCard(title: "활성 카페", value: "842", delta: "0.5%", icon: .cafe, trend: .up),
            AdminMetricCard(title: "승인 대기", value: "\(pendingCount)", delta: "\(pendingCount)건 대기", icon: .pending, trend: .new),
            AdminMetricCard(title: "신고 항목", value: "32", delta: "8%", icon: .report, trend: .down)
        ]
    }
}

private let defaultCafeRegistrationPendingRequests: [AdminPendingRequest] = [
    AdminPendingRequest(
        id: "pending-cafe-1",
        type: .cafeRegistration,
        title: "카페 모카라떼 홍대점",
        subtitle: "서울 마포구 어울마당로 123",
        requestedAt: "2시간 전",
        imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuAPTqu6TR5iE7rtn6cuSTaGUwIAdgNS9xaZqDyHkBXX25arxUP3ZAK6wS2HHUj-Efew3j9cuymLzCx7a7fUG8MqyZ1HFdgXXJoSTw9zIlWv0cvk_sjAIt-6daNAoEAg0lQTCaCkZ7CSKX2uNQpH9gyyUjrU2UdHrmBskzC9nIr06ms2YgAbzHhPdxZbEVZN41SPq6gUqSSTdRWJcI5AS-T3HTjq3n3yMJYZ7T_imgYTE1UrUdAnniws6bLwUzX_o9f7XcBOy5Ur9A"
    ),
    AdminPendingRequest(
        id: "pending-cafe-2",
        type: .cafeRegistration,
        title: "디저트 빌리지 성수",
        subtitle: "서울 성동구 아차산로 45",
        requestedAt: "5시간 전",
        imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuCe9Vib6B40fIweAG4csR1KYxnHTMoec_xzj6GS5343QHIszmvCk4_ZiPt1NOdpLruSfby0tdpH2myNthY3GZjMgDZw8Fjh70hjE55AGaHkmkMJdLkqsuISq4Gsa8WhO-JRD3SIBIY_FAoBdHYRxqq2AVZl7Xmrgp0OorSTkcVTdF6cO14mBMWbvzhU9Hga3y41jSo89iuQ8aG-D8oKHX5PPyeXXGllTSzc7oGE8PMT1rBx-DRviiY0QI2H9AvdAbcm8hHiBGfIVQ"
    )
]
