//
//  ReviewEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import Foundation

enum ReviewEditAction {
    case clickBack
    case selectRating(Int)
    case clickAddPhoto
    case removePhoto(String)
    case changeReviewText(String)
    case selectAtmosphereAnswer(Bool)
    case clickSubmit
    case dismissInfoMessage
}
