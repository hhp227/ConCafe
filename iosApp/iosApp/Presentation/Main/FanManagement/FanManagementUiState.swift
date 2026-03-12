//
//  FanManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct FanManagementUiState {
    var title: String = "팬 관리"
    var castProfile: CastProfile = .init(
        castId: "maid-1",
        cafeId: "cafe-1",
        stageName: "사쿠라",
        localizedName: "Sakura",
        cafeName: "메이드 하우스",
        profileAccent: "SK",
        isOnline: true
    )
    var stats: [StatCard] = [
        .init(label: "전체 팔로워", value: "1,240", highlight: .standard),
        .init(label: "오늘 신규", value: "+12", highlight: .primary),
        .init(label: "소통 지수", value: "98.5", highlight: .standard)
    ]
    var recentFollowers: [RecentFollower] = [
        .init(id: "ken", name: "Ken", joinedLabel: "방금 전", accent: true),
        .init(id: "sophie", name: "Sophie", joinedLabel: "2시간 전"),
        .init(id: "minjun", name: "Minjun", joinedLabel: "5시간 전"),
        .init(id: "hana", name: "Hana", joinedLabel: "어제")
    ]
    var topFans: [TopFan] = [
        .init(id: "master-k", rank: 1, name: "마스터 K", pointsLabel: "42,500P", isBest: true),
        .init(id: "moonlight", rank: 2, name: "달빛나그네", pointsLabel: "38,200P"),
        .init(id: "melodyfan", rank: 3, name: "멜로디팬", pointsLabel: "31,900P")
    ]
    var notificationCount: Int = 1
    var infoMessage: String? = "오늘 신규 팬 12명이 유입되었습니다."

    struct CastProfile {
        let castId: String
        let cafeId: String
        let stageName: String
        let localizedName: String
        let cafeName: String
        let profileAccent: String
        let isOnline: Bool
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

        var title: String {
            switch self {
            case .workSchedule:
                return "출근 관리"
            }
        }

        var subtitle: String {
            switch self {
            case .workSchedule:
                return "이번 주 스케줄을 조정합니다."
            }
        }
    }

    enum Highlight {
        case standard
        case primary
    }
}
