//
//  ChangePasswordAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

enum ChangePasswordAction {
    case backTapped
    case currentPasswordChanged(String)
    case newPasswordChanged(String)
    case confirmPasswordChanged(String)
    case submitTapped
}
