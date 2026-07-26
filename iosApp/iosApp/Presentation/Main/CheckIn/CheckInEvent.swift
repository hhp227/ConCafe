//
//  CheckInEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum CheckInEvent {
    case navigateToCafe(id: String)
    case navigateToCast(id: String)
    case navigateToReviewEdit(cafeId: String)
    case navigateToMap
    case navigateToSignIn
    case openLocationSettings
}
