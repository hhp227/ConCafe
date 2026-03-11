//
//  MenuGoodsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class MenuGoodsViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = MenuGoodsUiState()

    let event = PassthroughSubject<MenuGoodsEvent, Never>()

    private var sessionWatchHandle: WatchHandle?

    private var loadTask: Task<Void, Never>?

    private func loadMenuGoods() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.infoMessage = nil

        loadTask = Task {
            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CafeDetailFeed {
                    let detail = feed.detail
                    let menuItems = detail.menus.enumerated().map { index, menu in
                        mapMenuToManageItem(menu, index: index)
                    }
                    let goodsItems = detail.goods.enumerated().map { index, goods in
                        mapGoodsToManageItem(goods, index: index)
                    }

                    uiState.cafeName = detail.cafe.name
                    uiState.isLoading = false
                    uiState.menuCategories = buildMenuCategories(items: menuItems)
                    uiState.goodsCategories = buildGoodsCategories(items: goodsItems)
                    uiState.menuItems = menuItems
                    uiState.goodsItems = goodsItems
                } else {
                    uiState.isLoading = false
                    uiState.infoMessage = "메뉴와 굿즈 정보를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.infoMessage = "메뉴와 굿즈 정보를 불러오지 못했습니다."
            }
        }
    }

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadMenuGoods()
            }
        }
    }

    private func toggleSearch() {
        uiState.isSearchVisible.toggle()
        if !uiState.isSearchVisible {
            uiState.searchQuery = ""
        }
    }

    private func selectCategory(_ categoryId: String?) {
        switch uiState.selectedCollection {
        case .menu:
            uiState.selectedMenuCategoryId = categoryId
        case .goods:
            uiState.selectedGoodsCategoryId = categoryId
        }
    }

    private func toggleItemAvailability(_ itemId: String) {
        switch uiState.selectedCollection {
        case .menu:
            uiState.menuItems = uiState.menuItems.map { item in
                guard item.id == itemId else { return item }
                let nextAvailability = !item.isAvailable
                return MenuGoodsUiState.ManageItem(
                    id: item.id,
                    name: item.name,
                    priceText: item.priceText,
                    description: item.description,
                    imageUrl: item.imageUrl,
                    badgeLabel: item.badgeLabel,
                    categoryId: item.categoryId,
                    categoryLabel: item.categoryLabel,
                    isAvailable: nextAvailability,
                    availabilityLabel: nextAvailability ? "판매 중" : "품절",
                    inventoryLabel: item.inventoryLabel
                )
            }
        case .goods:
            uiState.goodsItems = uiState.goodsItems.map { item in
                guard item.id == itemId else { return item }
                let nextAvailability = !item.isAvailable
                return MenuGoodsUiState.ManageItem(
                    id: item.id,
                    name: item.name,
                    priceText: item.priceText,
                    description: item.description,
                    imageUrl: item.imageUrl,
                    badgeLabel: item.badgeLabel,
                    categoryId: item.categoryId,
                    categoryLabel: item.categoryLabel,
                    isAvailable: nextAvailability,
                    availabilityLabel: nextAvailability ? "판매 중" : "품절",
                    inventoryLabel: item.inventoryLabel
                )
            }
        }
    }

    private func showInfo(_ message: String) {
        uiState.infoMessage = message
    }

    private func buildMenuCategories(items: [MenuGoodsUiState.ManageItem]) -> [MenuGoodsUiState.CategoryChip] {
        let preferredOrder: [(String, MenuGoodsUiState.CategoryChip)] = [
            ("food", .init(id: "food", label: "Food", iconKey: "food")),
            ("drink", .init(id: "drink", label: "Drinks", iconKey: "drink")),
            ("dessert", .init(id: "dessert", label: "Dessert", iconKey: "dessert"))
        ]

        return [.init(id: nil, label: "All", iconKey: "all")] + preferredOrder.compactMap { id, chip in
            items.contains(where: { $0.categoryId == id }) ? chip : nil
        }
    }

    private func buildGoodsCategories(items: [MenuGoodsUiState.ManageItem]) -> [MenuGoodsUiState.CategoryChip] {
        var seen = Set<String>()
        let dynamic = items.compactMap { item -> MenuGoodsUiState.CategoryChip? in
            guard let categoryId = item.categoryId, !seen.contains(categoryId) else {
                return nil
            }
            seen.insert(categoryId)
            return MenuGoodsUiState.CategoryChip(
                id: categoryId,
                label: item.categoryLabel,
                iconKey: "goods"
            )
        }

        return [.init(id: nil, label: "All", iconKey: "all")] + dynamic
    }

    private func mapMenuToManageItem(_ menu: CafeMenu, index: Int) -> MenuGoodsUiState.ManageItem {
        let normalizedCategoryId = menu.category.lowercased()
        let categoryLabel: String
        switch normalizedCategoryId {
        case "food":
            categoryLabel = "Food"
        case "drink":
            categoryLabel = "Drinks"
        case "dessert":
            categoryLabel = "Dessert"
        default:
            categoryLabel = menu.category.capitalized
        }
        let isAvailable = !Self.soldOutMenuIds.contains(menu.id) && index % 5 != 4
        return MenuGoodsUiState.ManageItem(
            id: menu.id,
            name: menu.name,
            priceText: formatPrice(menu.price),
            description: menu.desc,
            imageUrl: menu.image,
            badgeLabel: categoryLabel,
            categoryId: normalizedCategoryId,
            categoryLabel: categoryLabel,
            isAvailable: isAvailable,
            availabilityLabel: isAvailable ? "판매 중" : "품절",
            inventoryLabel: nil
        )
    }

    private func mapGoodsToManageItem(_ goods: Goods, index: Int) -> MenuGoodsUiState.ManageItem {
        let category: String
        if goods.name.localizedCaseInsensitiveContains("포토") {
            category = "collectible"
        } else if goods.name.localizedCaseInsensitiveContains("의상") {
            category = "apparel"
        } else {
            category = "goods"
        }

        let categoryLabel: String
        switch category {
        case "collectible":
            categoryLabel = "Collectible"
        case "apparel":
            categoryLabel = "Apparel"
        default:
            categoryLabel = "Goods"
        }

        let isAvailable = goods.stock > 0 && index % 4 != 3
        return MenuGoodsUiState.ManageItem(
            id: goods.id,
            name: goods.name,
            priceText: formatPrice(goods.price),
            description: "카페 굿즈 판매 항목",
            imageUrl: goods.image,
            badgeLabel: categoryLabel,
            categoryId: category,
            categoryLabel: categoryLabel,
            isAvailable: isAvailable,
            availabilityLabel: isAvailable ? "판매 중" : "품절",
            inventoryLabel: "재고 \(goods.stock)"
        )
    }

    private func formatPrice(_ price: Int32) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        let number = NSNumber(value: price)
        let formatted = formatter.string(from: number) ?? "\(price)"
        return "KRW \(formatted)"
    }

    func onAction(_ action: MenuGoodsAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickSearch:
            toggleSearch()
        case .changeSearchQuery(let value):
            uiState.searchQuery = value
        case .selectCollection(let collection):
            uiState.selectedCollection = collection
        case .selectCategory(let categoryId):
            selectCategory(categoryId)
        case .toggleItemAvailability(let itemId):
            toggleItemAvailability(itemId)
        case .clickEditItem(_):
            showInfo("편집 기능은 다음 단계에서 연결됩니다.")
        case .clickDeleteItem(_):
            showInfo("삭제 확인 플로우는 다음 단계에서 연결됩니다.")
        case .clickAddNewItem:
            showInfo("신규 항목 등록 화면은 다음 단계에서 연결됩니다.")
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        loadMenuGoods()
    }

    deinit {
        sessionWatchHandle?.cancel()
        loadTask?.cancel()
    }

    private static let soldOutMenuIds: Set<String> = ["menu-1", "menu-4", "menu-8"]
}
