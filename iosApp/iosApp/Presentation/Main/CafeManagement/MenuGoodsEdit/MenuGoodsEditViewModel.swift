//
//  MenuGoodsEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class MenuGoodsEditViewModel: ObservableObject {
    private let cafeId: String

    private let itemId: String?

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase

    @Published private(set) var uiState = MenuGoodsEditUiState()

    let event = PassthroughSubject<MenuGoodsEditEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private func loadInitialValue() {
        guard let itemId else {
            uiState.isLoading = false
            uiState.isEditMode = false
            uiState.screenTitle = "새 항목 추가"
            uiState.saveButtonLabel = "항목 생성"
            return
        }

        loadTask?.cancel()
        uiState.isLoading = true
        uiState.infoMessage = nil

        loadTask = Task {
            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CafeDetailFeed {
                    let detail = feed.detail
                    uiState.detail = detail

                    if let menu = detail.menus.first(where: { $0.id == itemId }) {
                        applyMenu(menu)
                    } else if let goods = detail.goods.first(where: { $0.id == itemId }) {
                        applyGoods(goods)
                    } else {
                        showInfoAndStop("편집할 항목 정보를 찾을 수 없습니다.")
                    }
                } else {
                    showInfoAndStop("항목 정보를 불러오지 못했습니다.")
                }
            } catch {
                if Task.isCancelled { return }
                showInfoAndStop("항목 정보를 불러오지 못했습니다.")
            }
        }
    }

    private func saveItem() {
        guard !uiState.itemName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessage = "항목 이름을 입력해주세요."
            return
        }

        guard !uiState.price.isEmpty, let price = Int32(uiState.price) else {
            uiState.infoMessage = "가격을 입력해주세요."
            return
        }

        uiState.isSaving = true
        uiState.infoMessage = nil
        loadTask?.cancel()

        let update = CafeMenuGoodsUpsert(
            cafeId: cafeId,
            itemId: itemId,
            name: uiState.itemName,
            price: price,
            category: uiState.selectedCategory.categoryId,
            description: uiState.description,
            isInStock: uiState.isInStock,
            imageUrl: uiState.imageUrl
        )

        loadTask = Task {
            do {
                let result = try await upsertCafeMenuGoodsUseCase.invoke(update: update)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let detail = success.data as? CafeDetail {
                    uiState.detail = detail
                    uiState.isSaving = false
                    uiState.isLoading = false
                    uiState.isEditMode = true
                    uiState.screenTitle = "항목 편집"
                    uiState.saveButtonLabel = "항목 저장"
                    event.send(.navigateBack)
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = "항목 저장에 실패했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = "항목 저장에 실패했습니다."
            }
        }
    }

    private func applyMenu(_ menu: CafeMenu) {
        uiState.isLoading = false
        uiState.isEditMode = true
        uiState.screenTitle = "항목 편집"
        uiState.saveButtonLabel = "항목 저장"
        uiState.itemName = menu.name
        uiState.price = String(menu.price)
        uiState.selectedCategory = MenuGoodsEditUiState.ItemCategory(menuCategory: menu.category)
        uiState.description = menu.desc
        uiState.isInStock = menu.isAvailable
        uiState.imageUrl = menu.image
        uiState.infoMessage = nil
    }

    private func applyGoods(_ goods: Goods) {
        uiState.isLoading = false
        uiState.isEditMode = true
        uiState.screenTitle = "항목 편집"
        uiState.saveButtonLabel = "항목 저장"
        uiState.itemName = goods.name
        uiState.price = String(goods.price)
        uiState.selectedCategory = .goods
        uiState.description = "카페 굿즈 판매 항목"
        uiState.isInStock = goods.stock > 0
        uiState.imageUrl = goods.image
        uiState.infoMessage = nil
    }

    private func showInfoAndStop(_ message: String) {
        uiState.isLoading = false
        uiState.infoMessage = message
    }

    func onAction(_ action: MenuGoodsEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickPhotoUpload:
            uiState.infoMessage = "이미지 업로드는 다음 단계에서 연결됩니다."
        case .selectPhoto(let imageUrl):
            uiState.imageUrl = imageUrl
        case .changeName(let value):
            uiState.itemName = value
        case .changePrice(let value):
            uiState.price = String(value.filter(\.isNumber))
        case .selectCategory(let category):
            uiState.selectedCategory = category
        case .changeDescription(let value):
            uiState.description = value
        case .toggleStock(let isInStock):
            uiState.isInStock = isInStock
        case .clickSave:
            saveItem()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        cafeId: String,
        itemId: String? = nil,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase = KoinInitializerKt.resolveUpsertCafeMenuGoodsUseCase()
    ) {
        self.cafeId = cafeId
        self.itemId = itemId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.upsertCafeMenuGoodsUseCase = upsertCafeMenuGoodsUseCase

        loadInitialValue()
    }

    deinit {
        loadTask?.cancel()
    }
}

private extension MenuGoodsEditUiState.ItemCategory {
    init(menuCategory: String) {
        switch menuCategory.lowercased() {
        case "food":
            self = .food
        case "dessert":
            self = .dessert
        case "goods":
            self = .goods
        default:
            self = .drink
        }
    }
}
