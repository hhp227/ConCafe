//
//  MenuGoodsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class MenuGoodsViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let deleteCafeMenuGoodsUseCase: DeleteCafeMenuGoodsUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    @Published private(set) var uiState = MenuGoodsUiState()

    let event = PassthroughSubject<MenuGoodsEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadMenuGoods() {
        tasks[.load]?.cancel()
        uiState.isLoading = true
        uiState.infoMessage = nil

        tasks[.load] = Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CafeDetailFeed {
                    applyDetail(feed.detail)
                } else {
                    uiState.isLoading = false
                    uiState.infoMessage = "항목 정보를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.infoMessage = "항목 정보를 불러오지 못했습니다."
            }
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.detailEvent]?.cancel()
        tasks[.detailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    switch event {
                    case let event as CafeDetailEvent.CafeInfoUpdated:
                        if event.cafeId == self.cafeId {
                            self.loadMenuGoods()
                        }
                    case let event as CafeDetailEvent.MenuCreated:
                        if event.cafeId == self.cafeId {
                            self.loadMenuGoods()
                        }
                    case let event as CafeDetailEvent.MenuUpdated:
                        if event.cafeId == self.cafeId {
                            self.upsertLocalMenu(event.menu)
                        }
                    case let event as CafeDetailEvent.MenuDeleted:
                        if event.cafeId == self.cafeId {
                            self.removeLocalMenu(itemId: event.itemId)
                        }
                    case let event as CafeDetailEvent.GoodsCreated:
                        if event.cafeId == self.cafeId {
                            self.loadMenuGoods()
                        }
                    case let event as CafeDetailEvent.GoodsUpdated:
                        if event.cafeId == self.cafeId {
                            self.upsertLocalGoods(event.goods)
                        }
                    case let event as CafeDetailEvent.GoodsDeleted:
                        if event.cafeId == self.cafeId {
                            self.removeLocalGoods(itemId: event.itemId)
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func applyDetail(_ detail: CafeDetail) {
        let menuItems = detail.menus.enumerated().map { index, menu in
            self.mapMenuToManageItem(menu, index: index)
        }
        let goodsItems = detail.goods.enumerated().map { index, goods in
            self.mapGoodsToManageItem(goods, index: index)
        }

        var nextState = self.uiState
        nextState.cafeName = detail.cafe.name
        nextState.isLoading = false
        nextState.menuCategories = self.buildMenuCategories(items: menuItems)
        nextState.goodsCategories = self.buildGoodsCategories(items: goodsItems)
        nextState.menuItems = menuItems
        nextState.goodsItems = goodsItems
        nextState.infoMessage = nil
        self.uiState = nextState
    }

    private func upsertLocalMenu(_ menu: CafeMenu) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != menu.id } + [mapMenuToManageItem(menu, index: nextState.menuItems.count)]
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != menu.id }

        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.menuItems = nextMenuItems
        nextState.goodsItems = nextGoodsItems
        if nextState.pendingDeleteItem?.id == menu.id {
            nextState.pendingDeleteItem = nil
        }
        uiState = nextState
    }

    private func upsertLocalGoods(_ goods: Goods) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != goods.id }
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != goods.id } + [mapGoodsToManageItem(goods, index: nextState.goodsItems.count)]
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.menuItems = nextMenuItems
        nextState.goodsItems = nextGoodsItems
        if nextState.pendingDeleteItem?.id == goods.id {
            nextState.pendingDeleteItem = nil
        }
        uiState = nextState
    }

    private func removeLocalMenu(itemId: String) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != itemId }
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextState.goodsItems)
        nextState.menuItems = nextMenuItems
        if nextState.pendingDeleteItem?.id == itemId {
            nextState.pendingDeleteItem = nil
        }
        uiState = nextState
    }

    private func removeLocalGoods(itemId: String) {
        var nextState = uiState
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != itemId }
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextState.menuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.goodsItems = nextGoodsItems
        if nextState.pendingDeleteItem?.id == itemId {
            nextState.pendingDeleteItem = nil
        }
        uiState = nextState
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

    private func deleteItem(_ itemId: String) {
        guard let targetItem = uiState.visibleItems.first(where: { $0.id == itemId }) else { return }
        uiState.pendingDeleteItem = targetItem
    }

    private func confirmDeleteItem(_ itemId: String) {
        uiState.infoMessage = nil
        uiState.pendingDeleteItem = nil
        Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await deleteCafeMenuGoodsUseCase.invoke(cafeId: cafeId, itemId: itemId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.infoMessage = "항목이 삭제되었습니다."
                } else {
                    uiState.infoMessage = "항목 삭제에 실패했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = "항목 삭제에 실패했습니다."
            }
        }
    }

    private func cancelDeleteItem() {
        uiState.pendingDeleteItem = nil
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
        let isAvailable = menu.isAvailable
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

        let isAvailable = goods.stock > 0
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
        case .clickEditItem(let itemId):
            event.send(.navigateToEdit(cafeId: cafeId, itemId: itemId))
        case .clickDeleteItem(let itemId):
            deleteItem(itemId)
        case .confirmDeleteItem(let itemId):
            confirmDeleteItem(itemId)
        case .cancelDeleteItem:
            cancelDeleteItem()
        case .clickAddNewItem:
            event.send(.navigateToEdit(cafeId: cafeId, itemId: nil))
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        deleteCafeMenuGoodsUseCase: DeleteCafeMenuGoodsUseCase = KoinInitializerKt.resolveDeleteCafeMenuGoodsUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.deleteCafeMenuGoodsUseCase = deleteCafeMenuGoodsUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher

        observeCafeDetailEvent()
        loadMenuGoods()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case load
        case detailEvent
    }
}
