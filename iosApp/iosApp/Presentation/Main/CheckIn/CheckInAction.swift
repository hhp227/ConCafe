//
//  CheckInAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum CheckInAction {
    case cafeTapped(id: String)
    case castTapped(id: String)
    case checkInTapped
    case checkInForCafeTapped(cafeId: String)
    case signInTapped
    case signUpTapped
    case dismissLoginPrompt
    case dismissError
    case dismissNewVisitSheet
    case dismissQrCheckInSheet
    case dismissReviewPrompt
    case writeReviewPromptTapped
    case loadMoreRecentVisits
    case qrCheckInTapped
    case qrScanFailed(message: String)
    case submitQrCheckIn(rawValue: String)
    case submitNewVisit(cafeId: String, visitedAt: String, memo: String?)
}
