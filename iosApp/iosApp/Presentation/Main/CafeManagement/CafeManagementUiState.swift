//
//  CafeManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct CafeManagementUiState {
    struct OwnedCafe: Identifiable, Equatable {
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
        let externalLinkCount: Int
    }

    struct PendingClaim: Identifiable, Equatable {
        let id: String
        let cafeName: String
        let requestedAt: String
        let status: String
        let message: String
    }

    enum Shortcut: CaseIterable, Identifiable {
        case castManagement
        case castSchedule
        case eventManagement
        case cafeSettings
        case homeBanner
        case externalLinks

        var id: String { title }

        var title: String {
            switch self {
            case .castManagement: return "캐스트 관리"
            case .castSchedule: return "출근표"
            case .eventManagement: return "이벤트"
            case .cafeSettings: return "카페 설정"
            case .homeBanner: return "홈 배너"
            case .externalLinks: return "외부 링크"
            }
        }

        var subtitle: String {
            switch self {
            case .castManagement: return "프로필, 사진, 팬 연결"
            case .castSchedule: return "오늘 출근과 주간 일정"
            case .eventManagement: return "생일 이벤트와 시즌 행사"
            case .cafeSettings: return "기본 정보와 소개 관리"
            case .homeBanner: return "예약 배너와 진행 상태"
            case .externalLinks: return "Instagram, X, TikTok"
            }
        }

        var iconName: String {
            switch self {
            case .castManagement: return "person.3.fill"
            case .castSchedule: return "calendar"
            case .eventManagement: return "sparkles"
            case .cafeSettings: return "gearshape.fill"
            case .homeBanner: return "megaphone.fill"
            case .externalLinks: return "link"
            }
        }
    }

    var ownedCafes: [OwnedCafe] = []

    var pendingClaims: [PendingClaim] = []

    var selectedCafeId: String?

    var infoMessage: String?

    var selectedCafe: OwnedCafe? {
        ownedCafes.first(where: { $0.id == selectedCafeId }) ?? ownedCafes.first
    }

    var hasOwnedCafes: Bool {
        !ownedCafes.isEmpty
    }

    static let preview = CafeManagementUiState(
        ownedCafes: [
            OwnedCafe(
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
                externalLinkCount: 4
            ),
            OwnedCafe(
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
                externalLinkCount: 3
            ),
            OwnedCafe(
                id: "cafe-3",
                name: "Akihabara Butler Cafe",
                city: "Tokyo",
                isApproved: false,
                todayVisitors: 0,
                todayCheckIns: 0,
                todayReviews: 0,
                rating: 0,
                castCount: 3,
                noticeCount: 0,
                externalLinkCount: 1
            )
        ],
        pendingClaims: [
            PendingClaim(
                id: "claim-1",
                cafeName: "Ribbon Cafe Hongdae",
                requestedAt: "2026.03.10",
                status: "승인 대기 중",
                message: "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
            )
        ],
        selectedCafeId: "cafe-1",
        infoMessage: nil
    )
}
