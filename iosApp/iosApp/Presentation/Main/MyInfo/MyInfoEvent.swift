//
//  MyInfoEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum MyInfoEvent {
    case navigateToCafe(id: String)
    case navigateToCast(id: String)
    case navigateToSignIn
    case loggedOut
}
