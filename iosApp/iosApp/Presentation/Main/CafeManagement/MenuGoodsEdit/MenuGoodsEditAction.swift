//
//  MenuGoodsEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum MenuGoodsEditAction {
    case clickBack
    case clickPhotoUpload
    case changeName(String)
    case changePrice(String)
    case selectPhoto(String)
    case selectCategory(String)
    case changeDescription(String)
    case toggleStock(Bool)
    case clickSave
    case dismissInfoMessage
}
