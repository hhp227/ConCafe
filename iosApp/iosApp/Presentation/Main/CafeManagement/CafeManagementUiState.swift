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

    struct SearchableCafe: Identifiable, Equatable {
        let id: String
        let name: String
        let location: String
    }

    struct PendingClaim: Identifiable, Equatable {
        let id: String
        let cafeName: String
        let requestedAt: String
        let status: String
        let message: String
    }

    var ownedCafes: [OwnedCafe] = []

    var searchableCafes: [SearchableCafe] = []

    var pendingClaims: [PendingClaim] = []

    var isShowingAllCafes = false

    var cafeSearchQuery = ""

    var infoMessage: String?

    var featuredCafe: OwnedCafe? {
        ownedCafes.first
    }

    var hasOwnedCafes: Bool {
        !ownedCafes.isEmpty
    }

    var visibleOwnedCafes: [OwnedCafe] {
        isShowingAllCafes ? ownedCafes : Array(ownedCafes.prefix(Self.defaultVisibleCafeCount))
    }

    var hasHiddenOwnedCafes: Bool {
        ownedCafes.count > Self.defaultVisibleCafeCount
    }

    var filteredSearchableCafes: [SearchableCafe] {
        searchableCafes.filter { cafe in
            cafeSearchQuery.isEmpty ||
            cafe.name.localizedCaseInsensitiveContains(cafeSearchQuery) ||
            cafe.location.localizedCaseInsensitiveContains(cafeSearchQuery)
        }
    }

    private static let defaultVisibleCafeCount = 2

    static let preview = CafeManagementUiState(
        ownedCafes: [],
        searchableCafes: [
            SearchableCafe(id: "cafe-1", name: "Maid Dream Tokyo", location: "Tokyo Akihabara"),
            SearchableCafe(id: "cafe-2", name: "Seoul Maid Cafe", location: "서울 마포구 연남동"),
            SearchableCafe(id: "cafe-3", name: "Ribbon Cafe Hongdae", location: "서울 마포구 홍대입구"),
            SearchableCafe(id: "cafe-4", name: "Akihabara Butler Cafe", location: "Tokyo Chiyoda")
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
        isShowingAllCafes: false,
        cafeSearchQuery: "",
        infoMessage: nil
    )
}
