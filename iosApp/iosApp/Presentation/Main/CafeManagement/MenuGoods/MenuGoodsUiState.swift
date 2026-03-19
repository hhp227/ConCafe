//
//  MenuGoodsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct MenuGoodsUiState {
    var cafeName: String = ""
    var isLoading: Bool = true
    var isSearchVisible: Bool = false
    var searchQuery: String = ""
    var selectedCollection: CollectionTab = .menu
    var menuCategories: [CategoryChip] = []
    var goodsCategories: [CategoryChip] = []
    var selectedMenuCategoryId: String? = nil
    var selectedGoodsCategoryId: String? = nil
    var menuItems: [CafeMenu] = []
    var goodsItems: [Goods] = []
    var menuAvailabilityOverrides: [String: Bool] = [:]
    var goodsAvailabilityOverrides: [String: Bool] = [:]
    var infoMessage: String? = nil
    var pendingDeleteItemId: String? = nil

    enum CollectionTab {
        case menu
        case goods
    }

    struct CategoryChip: Hashable {
        let id: String?
        let label: String
        let iconKey: String
    }

    var visibleCategories: [CategoryChip] {
        switch selectedCollection {
        case .menu:
            return menuCategories
        case .goods:
            return goodsCategories
        }
    }

    var selectedCategoryId: String? {
        switch selectedCollection {
        case .menu:
            return selectedMenuCategoryId
        case .goods:
            return selectedGoodsCategoryId
        }
    }

    func isMenuAvailable(_ menu: CafeMenu) -> Bool {
        menuAvailabilityOverrides[menu.id] ?? menu.isAvailable
    }

    func isGoodsAvailable(_ goods: Goods) -> Bool {
        goodsAvailabilityOverrides[goods.id] ?? (goods.stock > 0)
    }

    func menuCategoryId(_ menu: CafeMenu) -> String {
        menu.category.lowercased()
    }

    func menuCategoryLabel(_ menu: CafeMenu) -> String {
        switch menuCategoryId(menu) {
        case "food":
            return "Food"
        case "drink":
            return "Drinks"
        case "dessert":
            return "Dessert"
        default:
            return menu.category.capitalized
        }
    }

    func goodsCategoryId(_ goods: Goods) -> String {
        if goods.name.localizedCaseInsensitiveContains("포토") {
            return "collectible"
        } else if goods.name.localizedCaseInsensitiveContains("의상") {
            return "apparel"
        }
        return "goods"
    }

    func goodsCategoryLabel(_ goods: Goods) -> String {
        switch goodsCategoryId(goods) {
        case "collectible":
            return "Collectible"
        case "apparel":
            return "Apparel"
        default:
            return "Goods"
        }
    }

    var filteredMenuItems: [CafeMenu] {
        let normalizedQuery = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        return menuItems.filter { item in
            let matchesCategory = selectedMenuCategoryId == nil || menuCategoryId(item) == selectedMenuCategoryId
            let matchesQuery =
                normalizedQuery.isEmpty ||
                item.name.localizedCaseInsensitiveContains(normalizedQuery) ||
                item.desc.localizedCaseInsensitiveContains(normalizedQuery) ||
                menuCategoryLabel(item).localizedCaseInsensitiveContains(normalizedQuery)
            return matchesCategory && matchesQuery
        }
    }

    var filteredGoodsItems: [Goods] {
        let normalizedQuery = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        return goodsItems.filter { item in
            let matchesCategory = selectedGoodsCategoryId == nil || goodsCategoryId(item) == selectedGoodsCategoryId
            let matchesQuery =
                normalizedQuery.isEmpty ||
                item.name.localizedCaseInsensitiveContains(normalizedQuery) ||
                "카페 굿즈 판매 항목".localizedCaseInsensitiveContains(normalizedQuery) ||
                goodsCategoryLabel(item).localizedCaseInsensitiveContains(normalizedQuery)
            return matchesCategory && matchesQuery
        }
    }
}
