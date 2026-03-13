//
//  AdminOperationsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct AdminOperationsUiState {
    var metrics: [AdminMetricCard] = defaultAdminMetrics
    var selectedPendingFilter: PendingFilter = .cafeRegistration
    var pendingRequests: [AdminPendingRequest] = defaultPendingRequests
    var quickMenus: [AdminQuickMenu] = defaultQuickMenus
    var hasUnreadNotifications: Bool = true
    var infoMessage: String? = nil

    var pendingFilters: [PendingFilterChip] {
        PendingFilter.allCases.map { filter in
            PendingFilterChip(
                filter: filter,
                label: filter.label,
                count: pendingRequests.filter { $0.type == filter }.count,
                isSelected: selectedPendingFilter == filter
            )
        }
    }

    var filteredPendingRequests: [AdminPendingRequest] {
        pendingRequests.filter { $0.type == selectedPendingFilter }
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

struct AdminPendingRequest: Identifiable {
    let id: String
    let type: PendingFilter
    let title: String
    let subtitle: String
    let requestedAt: String
    let imageUrl: String
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

private let defaultAdminMetrics: [AdminMetricCard] = [
    AdminMetricCard(title: "전체 사용자", value: "12,540", delta: "1.2%", icon: .users, trend: .up),
    AdminMetricCard(title: "활성 카페", value: "842", delta: "0.5%", icon: .cafe, trend: .up),
    AdminMetricCard(title: "승인 대기", value: "15", delta: "5건 신규", icon: .pending, trend: .new),
    AdminMetricCard(title: "신고 항목", value: "32", delta: "8%", icon: .report, trend: .down)
]

private let defaultPendingRequests: [AdminPendingRequest] = [
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
    ),
    AdminPendingRequest(
        id: "pending-role-1",
        type: .roleClaim,
        title: "점장 권한 신청 - 리본냥",
        subtitle: "메이드 하우스 운영 계정 전환 요청",
        requestedAt: "1시간 전",
        imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuAPTqu6TR5iE7rtn6cuSTaGUwIAdgNS9xaZqDyHkBXX25arxUP3ZAK6wS2HHUj-Efew3j9cuymLzCx7a7fUG8MqyZ1HFdgXXJoSTw9zIlWv0cvk_sjAIt-6daNAoEAg0lQTCaCkZ7CSKX2uNQpH9gyyUjrU2UdHrmBskzC9nIr06ms2YgAbzHhPdxZbEVZN41SPq6gUqSSTdRWJcI5AS-T3HTjq3n3yMJYZ7T_imgYTE1UrUdAnniws6bLwUzX_o9f7XcBOy5Ur9A"
    ),
    AdminPendingRequest(
        id: "pending-role-2",
        type: .roleClaim,
        title: "캐스트 권한 신청 - 사쿠라",
        subtitle: "메이드 하우스 캐스트 인증 요청",
        requestedAt: "3시간 전",
        imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuCe9Vib6B40fIweAG4csR1KYxnHTMoec_xzj6GS5343QHIszmvCk4_ZiPt1NOdpLruSfby0tdpH2myNthY3GZjMgDZw8Fjh70hjE55AGaHkmkMJdLkqsuISq4Gsa8WhO-JRD3SIBIY_FAoBdHYRxqq2AVZl7Xmrgp0OorSTkcVTdF6cO14mBMWbvzhU9Hga3y41jSo89iuQ8aG-D8oKHX5PPyeXXGllTSzc7oGE8PMT1rBx-DRviiY0QI2H9AvdAbcm8hHiBGfIVQ"
    )
]

private let defaultQuickMenus: [AdminQuickMenu] = [
    AdminQuickMenu(id: "banner", title: "홈 배너 관리", description: "이벤트 및 공지 배너 수정", icon: .banner, accent: .primary),
    AdminQuickMenu(id: "moderation", title: "신고 및 제재", description: "부적절한 컨텐츠 및 유저 차단", icon: .moderation, accent: .rose),
    AdminQuickMenu(id: "analytics", title: "시스템 통계", description: "유입 분석 및 매출 리포트", icon: .analytics, accent: .blue)
]
