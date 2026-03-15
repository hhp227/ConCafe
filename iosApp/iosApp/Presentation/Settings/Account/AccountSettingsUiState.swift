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
    var nickname: String
    var email: String
    var role: UserRole?
    var memberSince: String
    var roleSummary: String
    var linkedCafeName: String?
    var ownedCafeCount: Int
    var castId: String?
    var castCafeId: String?
    var castName: String
    var castConceptRole: String
    var castDescription: String
    var isDeleteDialogVisible: Bool
    var deleteConfirmation: String
    var isDeleteRequested: Bool

    static let empty = AccountSettingsUiState(
        isLoading: true,
        errorMessage: nil,
        nickname: "",
        email: "",
        role: nil,
        memberSince: "",
        roleSummary: "",
        linkedCafeName: nil,
        ownedCafeCount: 0,
        castId: nil,
        castCafeId: nil,
        castName: "",
        castConceptRole: "",
        castDescription: "",
        isDeleteDialogVisible: false,
        deleteConfirmation: "",
        isDeleteRequested: false
    )
}
