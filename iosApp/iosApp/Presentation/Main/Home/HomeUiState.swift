//
//  HomeUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct HomeUiState {
    var isLoading: Bool = false
    var isLoggedIn: Bool
    var isLoginPromptVisible: Bool
    var bannerLayout: AppBannerLayout = .fullBleed
    let banners: [HomeBanner]
    let popularCasts: [Cast]
    let popularCastCafeNames: [String: String]
    let popularCastCafeRegions: [String: String]
    let popularCastCursor: String?
    let canLoadMorePopularCasts: Bool
    let isLoadingMorePopularCasts: Bool
    let nearbyCafes: [Cafe]
    let nearbyCafeCursor: String?
    let canLoadMoreNearbyCafes: Bool
    let isLoadingMoreNearbyCafes: Bool
    let birthdayCasts: [Cast]
    let birthdayCastCafeNames: [String: String]
    let notices: [Notice]
    let cafeEvents: [HomeCafeEvent]
    let cafeEventCursor: String?
    let canLoadMoreCafeEvents: Bool
    let isLoadingMoreCafeEvents: Bool
    var communityPosts: [CommunityPost]

    static let empty = HomeUiState(
        isLoading: false,
        isLoggedIn: false,
        isLoginPromptVisible: false,
        bannerLayout: .fullBleed,
        banners: [],
        popularCasts: [],
        popularCastCafeNames: [:],
        popularCastCafeRegions: [:],
        popularCastCursor: nil,
        canLoadMorePopularCasts: false,
        isLoadingMorePopularCasts: false,
        nearbyCafes: [],
        nearbyCafeCursor: nil,
        canLoadMoreNearbyCafes: false,
        isLoadingMoreNearbyCafes: false,
        birthdayCasts: [],
        birthdayCastCafeNames: [:],
        notices: [],
        cafeEvents: [],
        cafeEventCursor: nil,
        canLoadMoreCafeEvents: false,
        isLoadingMoreCafeEvents: false,
        communityPosts: []
    )
}
