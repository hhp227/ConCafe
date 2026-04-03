//
//  CafeEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum CafeEvent {
    case navigateBack
    case navigateToCast(id: String)
    case navigateToReviewEdit(cafeId: String, reviewId: String?)
    case navigateToSignIn
    case showReviewDeleteFailedMessage
    case showReviewReportedMessage
}
