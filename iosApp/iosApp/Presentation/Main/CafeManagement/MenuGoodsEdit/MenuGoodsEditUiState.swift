//
//  MenuGoodsEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct MenuGoodsEditUiState {
    var detail: CafeDetail?
    var isLoading = true
    var isSaving = false
    var isEditMode = false
    var itemName = ""
    var price = ""
    var selectedCategoryId = "drink"
    var description = ""
    var isInStock = true
    var imageUrl: String?
    var infoMessageKey: String?
}
