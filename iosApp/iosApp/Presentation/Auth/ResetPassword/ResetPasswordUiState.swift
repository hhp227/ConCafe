//
//  ResetPasswordUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/28.
//

import Foundation

struct ResetPasswordUiState {
    var email: String

    var isSubmitting: Bool

    static let empty = ResetPasswordUiState(
        email: "",
        isSubmitting: false
    )
}
