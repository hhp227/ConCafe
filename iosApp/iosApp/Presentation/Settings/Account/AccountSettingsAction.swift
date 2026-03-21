//
//  AccountSettingsAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

enum AccountSettingsAction {
    case backTapped
    case nicknameChanged(String)
    case saveUserInfoTapped
    case openCastEditTapped
    case openChangePasswordTapped
    case showDeleteDialogTapped
    case dismissDeleteDialogTapped
    case deletePasswordChanged(String)
    case deleteAccountTapped
}
