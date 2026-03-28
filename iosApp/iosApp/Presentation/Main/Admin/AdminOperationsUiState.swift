//
//  AdminOperationsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct AdminOperationsUiState {
    var totalUsersCount: Int = 0
    var activeCafesCount: Int = 0
    var reportItemsCount: Int = 0
    var metrics: [AdminMetricCard] = buildAdminMetrics(
        totalUsersCount: 0,
        activeCafesCount: 0,
        pendingCount: 0,
        reportItemsCount: 0
    )
    var selectedPendingFilter: PendingFilter = .cafeRegistration
    var pendingCafeRegistrationClaims: [PendingCafeRegistrationClaimPreview] = []
    var pendingCafeOwnerClaims: [PendingCafeOwnerClaimPreview] = []
    var inquiries: [Inquiry] = []
    var inquiryNextCursor: String? = nil
    var canLoadMoreInquiries: Bool = false
    var isLoadingMoreInquiries: Bool = false
    var quickMenus: [AdminQuickMenu] = defaultQuickMenus
    var hasUnreadNotifications: Bool = true
    var infoMessage: String? = nil

    var pendingFilters: [PendingFilterChip] {
        PendingFilter.allCases.map { filter in
            PendingFilterChip(
                filter: filter,
                label: filter.label,
                count: filter == .cafeRegistration ? pendingCafeRegistrationClaims.count : pendingCafeOwnerClaims.count,
                isSelected: selectedPendingFilter == filter
            )
        }
    }
}

struct AdminMetricCard: Identifiable {
    let id = UUID()
    let title: String
    let value: String
    let delta: String
    let icon: AdminMetricIcon
    let trend: MetricTrend
}

struct PendingFilterChip: Identifiable {
    var id: PendingFilter { filter }
    let filter: PendingFilter
    let label: String
    let count: Int
    let isSelected: Bool
}

struct AdminQuickMenu: Identifiable {
    let id: String
    let title: String
    let description: String
    let icon: QuickMenuIcon
    let accent: QuickMenuAccent
}

enum PendingFilter: CaseIterable {
    case cafeRegistration
    case roleClaim

    var label: String {
        switch self {
        case .cafeRegistration: return "카페 등록"
        case .roleClaim: return "권한 신청"
        }
    }
}

enum MetricTrend {
    case up
    case down
    case new
}

enum AdminMetricIcon {
    case users
    case cafe
    case pending
    case report
}

enum QuickMenuIcon {
    case banner
    case moderation
    case analytics
}

enum QuickMenuAccent {
    case primary
    case rose
    case blue
}

func buildAdminMetrics(
    totalUsersCount: Int,
    activeCafesCount: Int,
    pendingCount: Int,
    reportItemsCount: Int
) -> [AdminMetricCard] {
    [
        AdminMetricCard(title: "전체 사용자", value: "\(totalUsersCount)", delta: "실시간", icon: .users, trend: .up),
        AdminMetricCard(title: "활성 카페", value: "\(activeCafesCount)", delta: "실시간", icon: .cafe, trend: .up),
        AdminMetricCard(title: "승인 대기", value: "\(pendingCount)", delta: "\(pendingCount)건 대기", icon: .pending, trend: .new),
        AdminMetricCard(title: "신고 항목", value: "\(reportItemsCount)", delta: "실시간", icon: .report, trend: .down)
    ]
}

private let defaultQuickMenus: [AdminQuickMenu] = [
    AdminQuickMenu(id: "banner", title: "홈 배너 관리", description: "이벤트 및 공지 배너 수정", icon: .banner, accent: .primary),
    AdminQuickMenu(id: "moderation", title: "신고 및 제재", description: "부적절한 컨텐츠 및 유저 차단", icon: .moderation, accent: .rose),
    AdminQuickMenu(id: "analytics", title: "시스템 통계", description: "유입 분석 및 매출 리포트", icon: .analytics, accent: .blue)
]
