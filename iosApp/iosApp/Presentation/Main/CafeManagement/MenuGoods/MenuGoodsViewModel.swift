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

    private let upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase

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
        var nextState = self.uiState
        nextState.cafeName = detail.cafe.name
        nextState.isLoading = false
        nextState.menuCategories = self.buildMenuCategories(items: detail.menus)
        nextState.goodsCategories = self.buildGoodsCategories(items: detail.goods)
        nextState.menuItems = detail.menus
        nextState.goodsItems = detail.goods
        nextState.menuAvailabilityOverrides = [:]
        nextState.goodsAvailabilityOverrides = [:]
        nextState.infoMessage = nil
        self.uiState = nextState
    }

    private func upsertLocalMenu(_ menu: CafeMenu) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != menu.id } + [menu]
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != menu.id }

        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.menuItems = nextMenuItems
        nextState.goodsItems = nextGoodsItems
        nextState.menuAvailabilityOverrides.removeValue(forKey: menu.id)
        nextState.goodsAvailabilityOverrides.removeValue(forKey: menu.id)
        if nextState.pendingDeleteItemId == menu.id { nextState.pendingDeleteItemId = nil }
        uiState = nextState
    }

    private func upsertLocalGoods(_ goods: Goods) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != goods.id }
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != goods.id } + [goods]
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.menuItems = nextMenuItems
        nextState.goodsItems = nextGoodsItems
        nextState.menuAvailabilityOverrides.removeValue(forKey: goods.id)
        nextState.goodsAvailabilityOverrides.removeValue(forKey: goods.id)
        if nextState.pendingDeleteItemId == goods.id { nextState.pendingDeleteItemId = nil }
        uiState = nextState
    }

    private func removeLocalMenu(itemId: String) {
        var nextState = uiState
        let nextMenuItems = nextState.menuItems.filter { $0.id != itemId }
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextMenuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextState.goodsItems)
        nextState.menuItems = nextMenuItems
        nextState.menuAvailabilityOverrides.removeValue(forKey: itemId)
        nextState.goodsAvailabilityOverrides.removeValue(forKey: itemId)
        if nextState.pendingDeleteItemId == itemId { nextState.pendingDeleteItemId = nil }
        uiState = nextState
    }

    private func removeLocalGoods(itemId: String) {
        var nextState = uiState
        let nextGoodsItems = nextState.goodsItems.filter { $0.id != itemId }
        nextState.isLoading = false
        nextState.menuCategories = buildMenuCategories(items: nextState.menuItems)
        nextState.goodsCategories = buildGoodsCategories(items: nextGoodsItems)
        nextState.goodsItems = nextGoodsItems
        nextState.menuAvailabilityOverrides.removeValue(forKey: itemId)
        nextState.goodsAvailabilityOverrides.removeValue(forKey: itemId)
        if nextState.pendingDeleteItemId == itemId { nextState.pendingDeleteItemId = nil }
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
            guard let item = uiState.menuItems.first(where: { $0.id == itemId }) else { return }
            let nextAvailability = !uiState.isMenuAvailable(item)
            uiState.menuAvailabilityOverrides[itemId] = nextAvailability
            uiState.infoMessage = nil
            tasks[.toggleAvailability]?.cancel()
            tasks[.toggleAvailability] = Task { [weak self] in
                guard let self else { return }
                do {
                    let result = try await upsertCafeMenuGoodsUseCase.invoke(
                        update: CafeMenuGoodsUpsert(
                            cafeId: cafeId,
                            itemId: item.id,
                            name: item.name,
                            price: item.price,
                            category: item.category,
                            description: item.desc,
                            isInStock: nextAvailability,
                            imageUrl: item.image
                        )
                    )
                    if result is AppResultFailure {
                        uiState.menuAvailabilityOverrides.removeValue(forKey: itemId)
                        uiState.infoMessage = "판매 상태 저장에 실패했습니다."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.menuAvailabilityOverrides.removeValue(forKey: itemId)
                    uiState.infoMessage = "판매 상태 저장에 실패했습니다."
                }
            }
        case .goods:
            guard let item = uiState.goodsItems.first(where: { $0.id == itemId }) else { return }
            let nextAvailability = !uiState.isGoodsAvailable(item)
            uiState.goodsAvailabilityOverrides[itemId] = nextAvailability
            uiState.infoMessage = nil
            tasks[.toggleAvailability]?.cancel()
            tasks[.toggleAvailability] = Task { [weak self] in
                guard let self else { return }
                do {
                    let result = try await upsertCafeMenuGoodsUseCase.invoke(
                        update: CafeMenuGoodsUpsert(
                            cafeId: cafeId,
                            itemId: item.id,
                            name: item.name,
                            price: item.price,
                            category: "goods",
                            description: "카페 굿즈 판매 항목",
                            isInStock: nextAvailability,
                            imageUrl: item.image
                        )
                    )
                    if result is AppResultFailure {
                        uiState.goodsAvailabilityOverrides.removeValue(forKey: itemId)
                        uiState.infoMessage = "판매 상태 저장에 실패했습니다."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.goodsAvailabilityOverrides.removeValue(forKey: itemId)
                    uiState.infoMessage = "판매 상태 저장에 실패했습니다."
                }
            }
        }
    }

    private func deleteItem(_ itemId: String) {
        uiState.pendingDeleteItemId = itemId
    }

    private func confirmDeleteItem(_ itemId: String) {
        uiState.infoMessage = nil
        uiState.pendingDeleteItemId = nil
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
        uiState.pendingDeleteItemId = nil
    }

    private func buildMenuCategories(items: [CafeMenu]) -> [MenuGoodsUiState.CategoryChip] {
        let preferredOrder: [(String, MenuGoodsUiState.CategoryChip)] = [
            ("food", .init(id: "food", label: "Food", iconKey: "food")),
            ("drink", .init(id: "drink", label: "Drinks", iconKey: "drink")),
            ("dessert", .init(id: "dessert", label: "Dessert", iconKey: "dessert"))
        ]
        return [.init(id: nil, label: "All", iconKey: "all")] + preferredOrder.compactMap { id, chip in
            items.contains(where: { $0.category.lowercased() == id }) ? chip : nil
        }
    }

    private func buildGoodsCategories(items: [Goods]) -> [MenuGoodsUiState.CategoryChip] {
        var seen = Set<String>()
        let dynamic = items.compactMap { item -> MenuGoodsUiState.CategoryChip? in
            let categoryId: String
            if item.name.localizedCaseInsensitiveContains("포토") {
                categoryId = "collectible"
            } else if item.name.localizedCaseInsensitiveContains("의상") {
                categoryId = "apparel"
            } else {
                categoryId = "goods"
            }
            guard !seen.contains(categoryId) else {
                return nil
            }
            seen.insert(categoryId)
            return MenuGoodsUiState.CategoryChip(
                id: categoryId,
                label: categoryId == "collectible" ? "Collectible" : (categoryId == "apparel" ? "Apparel" : "Goods"),
                iconKey: "goods"
            )
        }

        return [.init(id: nil, label: "All", iconKey: "all")] + dynamic
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
        upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase = KoinInitializerKt.resolveUpsertCafeMenuGoodsUseCase(),
        deleteCafeMenuGoodsUseCase: DeleteCafeMenuGoodsUseCase = KoinInitializerKt.resolveDeleteCafeMenuGoodsUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.upsertCafeMenuGoodsUseCase = upsertCafeMenuGoodsUseCase
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
        case toggleAvailability
    }
}
