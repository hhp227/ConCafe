//
//  ContentAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/29.
//

import Foundation

enum ContentAction {
    case checkAppUpdate(storePlatform: String, storeId: String, currentVersion: String)
    case syncPushToken(token: String)
    case refreshUnreadNotificationCount
}
