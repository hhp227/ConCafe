//
//  CastListEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/05.
//

import Foundation

enum CastListEvent {
    case navigateBack
    case navigateToCastEdit(cafeId: String, castId: String?)
    case navigateToSchedule(castId: String)
}
