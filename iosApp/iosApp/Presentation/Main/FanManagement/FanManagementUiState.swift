//
//  FanManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct FanManagementUiState {
    var isLoading: Bool = true
    var errorMessage: String? = nil
    var fanManagementData: FanManagementData? = nil
    var castClaimStatus: CastClaimStatusCard? = nil
    var castClaimSheet: CastClaimSheet? = nil
    var isClaimSheetVisible: Bool = false
    var stats: [StatCard] = []
    var recentFollowers: [RecentFollower] = []
    var topFans: [TopFan] = []
    var infoMessage: String? = nil

    struct CastClaimStatusCard: Hashable {
        let affiliatedCafeId: String
        let affiliatedCafeName: String
        let headline: String
        let body: String
        let accent: Accent
    }

    struct CastClaimSheet: Hashable {
        let affiliatedCafeId: String
        let affiliatedCafeName: String
        let headline: String
        let body: String
        let requestableCasts: [ClaimCandidate]
        let nextCursor: String?
        let canLoadMore: Bool
        let isLoadingMore: Bool
        let selectedCastId: String?
        let canSubmit: Bool
        let isSubmitting: Bool
    }

    struct ClaimCandidate: Identifiable, Hashable {
        let id: String
        let name: String
    }

    struct StatCard: Hashable {
        let label: String
        let value: String
        let highlight: Highlight
    }

    struct RecentFollower: Identifiable, Hashable {
        let id: String
        let name: String
        let joinedLabel: String
        let accent: Bool

        init(id: String, name: String, joinedLabel: String, accent: Bool = false) {
            self.id = id
            self.name = name
            self.joinedLabel = joinedLabel
            self.accent = accent
        }

        var initial: String {
            String(name.prefix(1)).uppercased()
        }
    }

    struct TopFan: Identifiable, Hashable {
        let id: String
        let rank: Int
        let name: String
        let pointsLabel: String
        let isBest: Bool

        init(id: String, rank: Int, name: String, pointsLabel: String, isBest: Bool = false) {
            self.id = id
            self.rank = rank
            self.name = name
            self.pointsLabel = pointsLabel
            self.isBest = isBest
        }
    }

    enum QuickAction: CaseIterable, Hashable {
        case workSchedule
        case cafeDashboard

        var title: String {
            switch self {
            case .workSchedule:
                return "출근 관리"
            case .cafeDashboard:
                return "프로필 연결"
            }
        }

        var subtitle: String {
            switch self {
            case .workSchedule:
                return "이번 주 스케줄을 조정합니다."
            case .cafeDashboard:
                return "내 캐스트 프로필 연결 상태를 관리합니다."
            }
        }
    }

    enum Highlight {
        case standard
        case primary
    }

    enum Accent {
        case pending
        case linked
        case rejected
    }

    static let empty = FanManagementUiState(
        isLoading: true,
        errorMessage: nil,
        fanManagementData: nil,
        castClaimStatus: nil,
        stats: [],
        recentFollowers: [],
        topFans: [],
        infoMessage: nil
    )
}
