//
//  MenuGoodsEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct MenuGoodsEditUiState {
    enum ItemCategory: String, CaseIterable, Identifiable {
        case drink
        case food
        case dessert
        case goods

        var id: String { rawValue }

        var label: String {
            switch self {
            case .drink:
                return "음료"
            case .food:
                return "음식"
            case .dessert:
                return "디저트"
            case .goods:
                return "굿즈"
            }
        }

        var categoryId: String { rawValue }
    }

    var detail: CafeDetail?
    var isLoading = true
    var isSaving = false
    var isEditMode = false
    var screenTitle = "새 항목 추가"
    var saveButtonLabel = "항목 생성"
    var itemName = ""
    var price = ""
    var selectedCategory = ItemCategory.drink
    var description = ""
    var isInStock = true
    var imageUrl: String?
    var infoMessage: String?
}
