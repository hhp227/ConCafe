//
//  CafeEventUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation
import Shared

struct CafeEventUiState {
    var isLoading: Bool = false
    var event: CafeEventManagementItem? = nil
    var errorMessage: String? = nil
    var participantCasts: [Cast] = []
    var isLikedByMe: Bool = false
    var likeCount: Int = 0
    var isTogglingLike: Bool = false

    static let empty = CafeEventUiState()
}
