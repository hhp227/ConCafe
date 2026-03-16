//
//  ChangePasswordUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

struct ChangePasswordUiState {
    var currentPassword: String
    var newPassword: String
    var confirmPassword: String

    static let empty = ChangePasswordUiState(
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
    )
}
