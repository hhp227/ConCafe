//
//  MenuGoodsAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum MenuGoodsAction {
    case clickBack
    case clickSearch
    case changeSearchQuery(String)
    case selectCollection(MenuGoodsUiState.CollectionTab)
    case selectCategory(String?)
    case toggleItemAvailability(String)
    case clickEditItem(String)
    case clickDeleteItem(String)
    case clickAddNewItem
    case dismissInfoMessage
}
