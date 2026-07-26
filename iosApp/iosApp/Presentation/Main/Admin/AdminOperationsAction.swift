//
//  AdminOperationsAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum AdminOperationsAction {
    case clickNotifications
    case clickSeeAllPending
    case clickBannerRegister
    case loadMoreInquiries
    case loadMoreReports
    case selectPendingFilter(PendingFilter)
    case approvePending(String)
    case rejectPending(String)
    case clickQuickMenu(String)
    case dismissInfoMessage
}
