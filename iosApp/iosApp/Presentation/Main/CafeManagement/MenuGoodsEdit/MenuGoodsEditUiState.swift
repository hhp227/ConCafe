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
    var screenTitle = "새 항목 추가"
    var saveButtonLabel = "항목 생성"
    var itemName = ""
    var price = ""
    var selectedCategoryId = "drink"
    var description = ""
    var isInStock = true
    var imageUrl: String?
    var infoMessage: String?
}
