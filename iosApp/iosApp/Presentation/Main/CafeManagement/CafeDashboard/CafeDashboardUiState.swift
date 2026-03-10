//
//  CafeDashboardUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation

struct CafeDashboardUiState {
    struct CafeSummary: Equatable {
        let id: String
        let name: String
        let city: String
        let isApproved: Bool
        let todayVisitors: Int
        let todayCheckIns: Int
        let todayReviews: Int
        let rating: Double
        let castCount: Int
        let noticeCount: Int
        let eventCount: Int
        let externalLinkCount: Int
        let castPreviews: [CastPreview]
        let homeBannerPreview: HomeBannerPreview
    }

    struct CastPreview: Identifiable, Equatable {
        let id: String
        let name: String
        let isOnShift: Bool
    }

    struct HomeBannerPreview: Equatable {
        let title: String
        let period: String
        let statusLabel: String
    }

    enum Shortcut: String, CaseIterable, Identifiable {
        case castManagement
        case castSchedule
        case eventManagement
        case cafeSettings
        case menuGoods
        case homeBanner
        case externalLinks

        var id: String { rawValue }

        var title: String {
            switch self {
            case .castManagement: return "캐스트 관리"
            case .castSchedule: return "출근표"
            case .eventManagement: return "공지&이벤트"
            case .cafeSettings: return "카페 정보 관리"
            case .menuGoods: return "메뉴&굿즈"
            case .homeBanner: return "홈 배너"
            case .externalLinks: return "외부 링크"
            }
        }
    }

    var cafe: CafeSummary = Self.sampleCafes.first!

    var infoMessage: String?

    static func preview(cafeId: String) -> CafeDashboardUiState {
        CafeDashboardUiState(cafe: sampleCafes.first(where: { $0.id == cafeId }) ?? sampleCafes.first!)
    }

    private static let sampleCafes: [CafeSummary] = [
        CafeSummary(
            id: "cafe-1",
            name: "Maid Dream Tokyo",
            city: "Tokyo",
            isApproved: true,
            todayVisitors: 18,
            todayCheckIns: 12,
            todayReviews: 3,
            rating: 4.7,
            castCount: 8,
            noticeCount: 2,
            eventCount: 2,
            externalLinkCount: 4,
            castPreviews: [
                CastPreview(id: "cast-1", name: "미유", isOnShift: true),
                CastPreview(id: "cast-2", name: "하나", isOnShift: false),
                CastPreview(id: "cast-3", name: "리코", isOnShift: true)
            ],
            homeBannerPreview: HomeBannerPreview(
                title: "여름 한정 신메뉴 출시!",
                period: "2026.06.01 - 2026.08.31",
                statusLabel: "노출 중"
            )
        ),
        CafeSummary(
            id: "cafe-2",
            name: "Seoul Maid Cafe",
            city: "Seoul",
            isApproved: true,
            todayVisitors: 11,
            todayCheckIns: 7,
            todayReviews: 1,
            rating: 4.5,
            castCount: 5,
            noticeCount: 1,
            eventCount: 1,
            externalLinkCount: 3,
            castPreviews: [
                CastPreview(id: "cast-4", name: "사나", isOnShift: true),
                CastPreview(id: "cast-5", name: "유리", isOnShift: true),
                CastPreview(id: "cast-6", name: "노아", isOnShift: false)
            ],
            homeBannerPreview: HomeBannerPreview(
                title: "주말 콜라보 디저트 오픈",
                period: "2026.03.14 - 2026.03.31",
                statusLabel: "예약 중"
            )
        ),
        CafeSummary(
            id: "cafe-3",
            name: "Akihabara Butler Cafe",
            city: "Tokyo",
            isApproved: false,
            todayVisitors: 0,
            todayCheckIns: 0,
            todayReviews: 0,
            rating: 0.0,
            castCount: 3,
            noticeCount: 0,
            eventCount: 0,
            externalLinkCount: 1,
            castPreviews: [
                CastPreview(id: "cast-7", name: "레이", isOnShift: false),
                CastPreview(id: "cast-8", name: "카렌", isOnShift: false)
            ],
            homeBannerPreview: HomeBannerPreview(
                title: "신규 오픈 안내 배너",
                period: "2026.03.20 - 2026.04.20",
                statusLabel: "검수 중"
            )
        )
    ]
}
