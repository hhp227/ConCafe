//
//  AccountSettingsEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

enum AccountSettingsEvent {
    case navigateBack
    case navigateToCastEdit(cafeId: String?, castId: String?)
    case navigateToChangePassword
    case showMessage(String)
}
