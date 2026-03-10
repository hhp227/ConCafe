//
//  SignUpAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

enum SignUpAction {
    case backTapped
    case userTypeTapped(SignUpUiState.UserType)
    case backToTypeSelectionTapped
    case emailChanged(String)
    case passwordChanged(String)
    case confirmPasswordChanged(String)
    case nicknameChanged(String)
    case nameChanged(String)
    case phoneChanged(String)
    case verificationCodeChanged(String)
    case cafeSearchQueryChanged(String)
    case sendVerificationTapped
    case verifyCodeTapped
    case toggleCafeSearchTapped
    case cafeTapped(Cafe)
    case clearCafeTapped
    case submitTapped
    case socialSignUpTapped(provider: SignUpProvider)
    case signInInsteadTapped
}

enum SignUpProvider: String {
    case kakao
    case google
    case apple
}
