//
//  MenuGoodsEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit

struct MenuGoodsEditView: View {
    let cafeId: String

    let itemId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: MenuGoodsEditViewModel

    @State private var isPhotoPickerPresented = false

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.uiState.isEditMode ? String(localized: String.LocalizationValue("menugoods_edit_title_edit"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_edit_title_add"), table: "Localizable"))
                        .font(.title2.weight(.bold))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(viewModel.uiState.isEditMode ? String(localized: String.LocalizationValue("menugoods_edit_subtitle_edit"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_edit_subtitle_add"), table: "Localizable"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let infoMessageKey = viewModel.uiState.infoMessageKey {
                        Text(
                            {
                                switch infoMessageKey {
                                case "menugoods_edit_info_item_not_found",
                                     "menugoods_edit_info_load_failed",
                                     "menugoods_edit_info_enter_name",
                                     "menugoods_edit_info_enter_price",
                                     "menugoods_edit_info_price_number_only",
                                     "menugoods_edit_info_save_failed",
                                     "menugoods_edit_info_image_upload_failed",
                                     "menugoods_edit_info_image_upload_next_step":
                                    return String(localized: String.LocalizationValue(infoMessageKey), table: "Localizable")
                                default:
                                    return infoMessageKey
                                }
                            }()
                        )
                            .font(.footnote)
                            .foregroundStyle(ConCafeColors.goldDeep)
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(ConCafeColors.goldContainer)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    photoUploadSection
                    Group {
                        ConCafeFormField(
                            label: String(localized: String.LocalizationValue("menugoods_edit_label_name"), table: "Localizable"),
                            text: Binding(
                                get: { viewModel.uiState.itemName },
                                set: { viewModel.onAction(.changeName($0)) }
                            )
                        )
                        ConCafeFormField(
                            label: String(localized: String.LocalizationValue("menugoods_edit_label_price"), table: "Localizable"),
                            text: Binding(
                                get: { viewModel.uiState.price },
                                set: { viewModel.onAction(.changePrice($0)) }
                            ),
                            leadingContent: {
                                Text("₩")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(ConCafeColors.textSecondary)
                            }
                        )
                        .keyboardType(.numberPad)
                        ConCafeFormEditor(
                            label: String(localized: String.LocalizationValue("menugoods_edit_label_desc"), table: "Localizable"),
                            text: Binding(
                                get: { viewModel.uiState.description },
                                set: { viewModel.onAction(.changeDescription($0)) }
                            )
                        )
                        categorySection
                        Toggle(
                            String(localized: String.LocalizationValue("menugoods_edit_stock_toggle"), table: "Localizable"),
                            isOn: Binding(
                                get: { viewModel.uiState.isInStock },
                                set: { viewModel.onAction(.toggleStock($0)) }
                            )
                        )
                    }
                }
                .padding(16)
            }
            bottomSaveBar()
        }
        .navigationTitle(viewModel.uiState.isEditMode ? String(localized: String.LocalizationValue("menugoods_edit_title_edit"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_edit_title_add"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
        .sheet(isPresented: $isPhotoPickerPresented) {
            CompatImagePicker(onImageSelected: { image in
                isPhotoPickerPresented = false
                saveImageToTemporaryFileAsync(image) { imageUrl in
                    if let imageUrl {
                        viewModel.onAction(.selectPhoto(imageUrl))
                    }
                }
            }, onDismiss: {
                isPhotoPickerPresented = false
            })
        }
    }

    init(
        cafeId: String,
        itemId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.itemId = itemId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: MenuGoodsEditViewModel(cafeId: cafeId, itemId: itemId))
    }

    private func bottomSaveBar() -> some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(ConCafeColors.primaryContainer.opacity(0.2))
                .frame(height: 1)
            Button {
                viewModel.onAction(.clickSave)
            } label: {
                HStack {
                    Spacer()
                    if viewModel.uiState.isSaving {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Text(viewModel.uiState.isEditMode ? String(localized: String.LocalizationValue("menugoods_edit_save_update"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_edit_save_create"), table: "Localizable"))
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                }
                .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(ConCafeColors.primaryContainer)
            .foregroundStyle(ConCafeColors.textPrimary)
            .disabled(viewModel.uiState.isSaving || viewModel.uiState.isLoading)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(
                UITraitCollection.current.userInterfaceStyle == .dark
                ? ConCafeColors.background
                : Color.white.opacity(0.92)
            )
        }
    }

    private var categorySection: some View {
        let categoryIds = ["drink", "food", "dessert", "goods"]
        return VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("menugoods_edit_label_category"), table: "Localizable"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(ConCafeColors.textSecondary)
            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12)
                ],
                spacing: 12
            ) {
                ForEach(categoryIds, id: \.self) { categoryId in
                    categoryButton(categoryId: categoryId)
                }
            }
        }
    }

    private func categoryButton(categoryId: String) -> some View {
        let isSelected = viewModel.uiState.selectedCategoryId == categoryId
        return Button {
            viewModel.onAction(.selectCategory(categoryId))
        } label: {
            HStack(spacing: 8) {
                Image(systemName: categorySymbolName(categoryId: categoryId))
                    .font(.subheadline.weight(.semibold))
                Text(categoryLabel(categoryId: categoryId))
                    .font(.subheadline.weight(.medium))
            }
            .foregroundStyle(isSelected ? ConCafeColors.textPrimary : ConCafeColors.textSecondary)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(isSelected ? ConCafeColors.primaryContainer.opacity(0.2) : ConCafeColors.surfaceVariant)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(
                        isSelected ? ConCafeColors.primaryContainer : ConCafeColors.primaryContainer.opacity(0.3),
                        lineWidth: isSelected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private func categoryLabel(categoryId: String) -> String {
        switch categoryId {
        case "food":
            return String(localized: String.LocalizationValue("menugoods_edit_category_food"), table: "Localizable")
        case "dessert":
            return String(localized: String.LocalizationValue("menugoods_edit_category_dessert"), table: "Localizable")
        case "goods":
            return String(localized: String.LocalizationValue("menugoods_edit_category_goods"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("menugoods_edit_category_drink"), table: "Localizable")
        }
    }

    private func categorySymbolName(categoryId: String) -> String {
        switch categoryId {
        case "drink":
            return "cup.and.saucer.fill"
        case "food":
            return "fork.knife"
        case "dessert":
            return "birthday.cake.fill"
        case "goods":
            return "shippingbox.fill"
        default:
            return "cup.and.saucer.fill"
        }
    }

    private var photoUploadSection: some View {
        let imageUrl = viewModel.uiState.imageUrl
        return Button {
            isPhotoPickerPresented = true
        } label: {
            GeometryReader { proxy in
                ZStack {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [ConCafeColors.primaryContainer, ConCafeColors.surfaceTint],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    if let imageUrl, let url = URL(string: imageUrl), !imageUrl.isEmpty {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                loadingPhotoPlaceholder
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: proxy.size.width, height: proxy.size.height)
                                    .clipped()
                            case .failure:
                                loadingPhotoPlaceholder
                            @unknown default:
                                loadingPhotoPlaceholder
                            }
                        }
                    } else {
                        photoUploadPlaceholder
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(ConCafeColors.primaryContainer, lineWidth: 1.5)
                )
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private var loadingPhotoPlaceholder: some View {
        VStack(spacing: 8) {
            ProgressView()
                .tint(ConCafeColors.textMuted)
            Text(String(localized: String.LocalizationValue("menugoods_edit_image_loading"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(ConCafeColors.textMuted)
        }
    }

    private var photoUploadPlaceholder: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.badge.plus")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(ConCafeColors.primary)
            Text(String(localized: String.LocalizationValue("menugoods_edit_upload_title"), table: "Localizable"))
                .font(.subheadline.weight(.bold))
                .foregroundStyle(ConCafeColors.textSecondary)
            Text(String(localized: String.LocalizationValue("menugoods_edit_upload_desc"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(ConCafeColors.textMuted)
        }
    }

    private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
        saveCompressedImageToTemporaryFile(image)
    }

    private func saveImageToTemporaryFileAsync(
        _ image: UIImage,
        completion: @escaping (String?) -> Void
    ) {
        saveCompressedImageToTemporaryFileAsync(image, completion: completion)
    }

}

struct MenuGoodsEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            MenuGoodsEditView(cafeId: "cafe-1", itemId: nil, onNavigationAction: { _ in })
        }
    }
}
