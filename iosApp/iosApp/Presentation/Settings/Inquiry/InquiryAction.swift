//
//  InquiryAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

enum InquiryAction {
    case backTapped
    case inquiryTypeChanged(InquiryType)
    case titleChanged(String)
    case messageChanged(String)
    case submitTapped
}
