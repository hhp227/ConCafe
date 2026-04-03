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

    private let uploadImageUseCase: UploadImageUseCase

    @Published private(set) var uiState = MenuGoodsEditUiState()

    let event = PassthroughSubject<MenuGoodsEditEvent, Never>()

    private func loadInitialValue() {
        guard let itemId else {
            uiState.isLoading = false
            uiState.isEditMode = false
            return
        }
        uiState.isLoading = true
        uiState.infoMessageKey = nil

        Task {
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
                        showInfoAndStop(MessageKey.itemNotFound)
                    }
                } else {
                    showInfoAndStop(MessageKey.loadFailed)
                }
            } catch {
                if Task.isCancelled { return }
                showInfoAndStop(MessageKey.loadFailed)
            }
        }
    }

    private func saveItem() {
        guard !uiState.itemName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessageKey = MessageKey.enterName
            return
        }

        guard !uiState.price.isEmpty, let price = Int32(uiState.price) else {
            uiState.infoMessageKey = MessageKey.enterPrice
            return
        }

        uiState.isSaving = true
        uiState.infoMessageKey = nil

        Task {
            do {
                let uploadedImageUrl = try await uploadImageIfNeeded(uiState.imageUrl, folder: "cafe-items")
                let update = CafeMenuGoodsUpsert(
                    cafeId: cafeId,
                    itemId: itemId,
                    name: uiState.itemName,
                    price: price,
                    category: uiState.selectedCategoryId,
                    description: uiState.description,
                    isInStock: uiState.isInStock,
                    imageUrl: uploadedImageUrl
                )
                let result = try await upsertCafeMenuGoodsUseCase.invoke(update: update)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let detail = success.data as? CafeDetail {
                    uiState.detail = detail
                    uiState.isSaving = false
                    uiState.isLoading = false
                    uiState.isEditMode = true
                    event.send(.navigateBack)
                } else {
                    uiState.isSaving = false
                    uiState.infoMessageKey = MessageKey.saveFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessageKey = MessageKey.saveFailed
            }
        }
    }

    private func applyMenu(_ menu: CafeMenu) {
        uiState.isLoading = false
        uiState.isEditMode = true
        uiState.itemName = menu.name
        uiState.price = String(menu.price)
        uiState.selectedCategoryId = normalizeCategoryId(menu.category)
        uiState.description = menu.desc
        uiState.isInStock = menu.isAvailable
        uiState.imageUrl = menu.image
        uiState.infoMessageKey = nil
    }

    private func applyGoods(_ goods: Goods) {
        uiState.isLoading = false
        uiState.isEditMode = true
        uiState.itemName = goods.name
        uiState.price = String(goods.price)
        uiState.selectedCategoryId = "goods"
        uiState.description = "카페 굿즈 판매 항목"
        uiState.isInStock = goods.stock > 0
        uiState.imageUrl = goods.image
        uiState.infoMessageKey = nil
    }

    private func showInfoAndStop(_ messageKey: String) {
        uiState.isLoading = false
        uiState.infoMessageKey = messageKey
    }

    func onAction(_ action: MenuGoodsEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickPhotoUpload:
            uiState.infoMessageKey = MessageKey.imageUploadNextStep
        case .selectPhoto(let imageUrl):
            uiState.imageUrl = imageUrl
        case .changeName(let value):
            uiState.itemName = value
        case .changePrice(let value):
            uiState.price = String(value.filter(\.isNumber))
        case .selectCategory(let category):
            uiState.selectedCategoryId = normalizeCategoryId(category)
        case .changeDescription(let value):
            uiState.description = value
        case .toggleStock(let isInStock):
            uiState.isInStock = isInStock
        case .clickSave:
            saveItem()
        case .dismissInfoMessage:
            uiState.infoMessageKey = nil
        }
    }

    init(
        cafeId: String,
        itemId: String? = nil,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase = KoinInitializerKt.resolveUpsertCafeMenuGoodsUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.cafeId = cafeId
        self.itemId = itemId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.upsertCafeMenuGoodsUseCase = upsertCafeMenuGoodsUseCase
        self.uploadImageUseCase = uploadImageUseCase

        loadInitialValue()
    }

    private func normalizeCategoryId(_ categoryId: String) -> String {
        let normalized = categoryId.lowercased()
        switch normalized {
        case "drink", "food", "dessert", "goods":
            return normalized
        default:
            return "drink"
        }
    }

    private func uploadImageIfNeeded(_ imageUrl: String?, folder: String) async throws -> String? {
        guard let imageUrl, !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        let result = try await uploadImageUseCase.invoke(localPath: imageUrl, folder: folder)
        if let success = result as? AppResultSuccess<AnyObject>, let data = success.data as? String {
            return data
        }
        if let failure = result as? AppResultFailure, let validation = failure.error as? AppErrorValidationFailed {
            throw NSError(domain: "MenuGoodsEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: validation.reason])
        }
        throw NSError(domain: "MenuGoodsEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: String(localized: String.LocalizationValue(MessageKey.imageUploadFailed), table: "Localizable")])
    }

    private enum MessageKey {
        static let itemNotFound = "menugoods_edit_info_item_not_found"
        static let loadFailed = "menugoods_edit_info_load_failed"
        static let enterName = "menugoods_edit_info_enter_name"
        static let enterPrice = "menugoods_edit_info_enter_price"
        static let saveFailed = "menugoods_edit_info_save_failed"
        static let imageUploadNextStep = "menugoods_edit_info_image_upload_next_step"
        static let imageUploadFailed = "menugoods_edit_info_image_upload_failed"
    }
}
