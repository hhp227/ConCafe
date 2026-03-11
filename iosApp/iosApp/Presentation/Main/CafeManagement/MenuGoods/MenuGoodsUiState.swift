//
//  MenuGoodsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

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
    var menuItems: [ManageItem] = []
    var goodsItems: [ManageItem] = []
    var infoMessage: String? = nil

    enum CollectionTab {
        case menu
        case goods
    }

    struct CategoryChip: Hashable {
        let id: String?
        let label: String
        let iconKey: String
    }

    struct ManageItem: Identifiable, Hashable {
        let id: String
        let name: String
        let priceText: String
        let description: String
        let imageUrl: String?
        let badgeLabel: String
        let categoryId: String?
        let categoryLabel: String
        let isAvailable: Bool
        let availabilityLabel: String
        let inventoryLabel: String?
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

    var visibleItems: [ManageItem] {
        switch selectedCollection {
        case .menu:
            return menuItems
        case .goods:
            return goodsItems
        }
    }

    var filteredVisibleItems: [ManageItem] {
        visibleItems.filter { item in
            let matchesCategory = selectedCategoryId == nil || item.categoryId == selectedCategoryId
            let normalizedQuery = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesQuery =
                normalizedQuery.isEmpty ||
                item.name.localizedCaseInsensitiveContains(normalizedQuery) ||
                item.description.localizedCaseInsensitiveContains(normalizedQuery) ||
                item.categoryLabel.localizedCaseInsensitiveContains(normalizedQuery)
            return matchesCategory && matchesQuery
        }
    }
}
