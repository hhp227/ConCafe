//
//  CommunityUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import Foundation
import Shared

struct CommunityUiState {
    var isLoading: Bool = false
    var posts: [CommunityPost] = []
    var nextCursor: String? = nil
    var hasNext: Bool = false
    var isLoadingMore: Bool = false
    var errorMessage: String? = nil
    var nativeAds: [Int32: any NativeAdHandle] = [:]
}
