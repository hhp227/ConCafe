//
//  ExternalLinkUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

struct ExternalLinkUiState {
    let title: String
    let url: String

    var displayTitle: String {
        title.isEmpty ? "외부 링크" : title
    }
}
