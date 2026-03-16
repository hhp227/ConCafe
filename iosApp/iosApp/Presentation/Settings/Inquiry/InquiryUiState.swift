//
//  InquiryUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

struct InquiryUiState {
    var inquiryType: InquiryType = .service
    var title: String = ""
    var message: String = ""
    var isSubmitting: Bool = false
    var errorMessage: String?

    static let empty = InquiryUiState()
}

enum InquiryType: String, CaseIterable {
    case service
    case bugReport
    case suggestion

    var title: String {
        switch self {
        case .service:
            return "고객지원"
        case .bugReport:
            return "오류 제보"
        case .suggestion:
            return "서비스 제안"
        }
    }
}
