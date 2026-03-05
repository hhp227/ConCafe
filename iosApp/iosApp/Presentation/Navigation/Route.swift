//
//  Route.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation

enum Route: Hashable {
    case entry
    case main(initialTab: String?)
    case detail(param: String)
    case notification
}
