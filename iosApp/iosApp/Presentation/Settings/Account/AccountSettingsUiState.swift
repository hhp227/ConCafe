//
//  AccountSettingsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Shared

struct AccountSettingsUiState {
    var isLoading: Bool
    var errorMessage: String?
    var myInfoFeed: Shared.MyInfoFeed?
    var nicknameInput: String
    var isDeleteDialogVisible: Bool
    var deleteConfirmation: String
    var isDeleteRequested: Bool

    var role: UserRole? {
        myInfoFeed?.user?.role
    }

    static let empty = AccountSettingsUiState(
        isLoading: true,
        errorMessage: nil,
        myInfoFeed: nil,
        nicknameInput: "",
        isDeleteDialogVisible: false,
        deleteConfirmation: "",
        isDeleteRequested: false
    )
}
