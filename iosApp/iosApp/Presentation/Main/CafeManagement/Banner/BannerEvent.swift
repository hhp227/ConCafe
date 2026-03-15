//
//  BannerEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

enum BannerEvent {
    case navigateBack
    case navigateToBannerEdit(cafeId: String?)
    case showMessage(String)
}
