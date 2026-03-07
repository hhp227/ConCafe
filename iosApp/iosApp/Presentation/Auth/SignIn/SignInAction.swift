//
//  SignInAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum SignInAction {
    case emailChanged(String)
    case passwordChanged(String)
    case signInTapped
    case socialSignInTapped(provider: SignInProvider)
}

enum SignInProvider: String {
    case kakao
    case google
    case apple
}
