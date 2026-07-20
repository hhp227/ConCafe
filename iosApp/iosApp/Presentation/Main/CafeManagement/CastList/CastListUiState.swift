//
//  CastListUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/05.
//

import Foundation
import Shared

struct CastListUiState {
    var casts: [CafeCastPreview] = []
    var searchQuery = ""
    var deleteTargetCastId: String?
    var nextCursor: String?
    var hasMoreCasts = false
    var isLoadingMore = false
    var isLoading = true
    var infoMessage: String?

    var filteredCasts: [CafeCastPreview] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)

        if query.isEmpty {
            return casts
        } else {
            return casts.filter { $0.name.localizedCaseInsensitiveContains(query) }
        }
    }

    var isSearching: Bool {
        !searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var isDeleteCastDialogVisible: Bool {
        deleteTargetCastId != nil
    }
}
