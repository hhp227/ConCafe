//
//  HomeEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum HomeEvent {
    case openExternalLink(url: String)
    case navigateToCast(id: String)
    case navigateToCafe(id: String)
}
