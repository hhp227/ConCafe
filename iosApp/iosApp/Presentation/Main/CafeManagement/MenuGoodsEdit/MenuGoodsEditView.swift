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

    @State private var keyboardOverlap: CGFloat = 0

    var body: some View {
        GeometryReader { proxy in
            let safeAreaBottom = proxy.safeAreaInsets.bottom
            let keyboardBottomInset = max(0, keyboardOverlap - safeAreaBottom)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.uiState.screenTitle)
                        .font(.title2.weight(.bold))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Text(viewModel.uiState.isEditMode ? "항목 정보를 수정합니다." : "새 항목을 등록합니다.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    if let infoMessage = viewModel.uiState.infoMessage {
                        Text(infoMessage)
                            .font(.footnote)
                            .foregroundStyle(Color(hex: "6B5320"))
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(hex: "FFF6D7"))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    photoUploadSection
                    Group {
                        ConCafeFormField(
                            label: "항목명",
                            text: Binding(
                                get: { viewModel.uiState.itemName },
                                set: { viewModel.onAction(.changeName($0)) }
                            )
                        )
                        ConCafeFormField(
                            label: "가격",
                            text: Binding(
                                get: { viewModel.uiState.price },
                                set: { viewModel.onAction(.changePrice($0)) }
                            ),
                            leadingContent: {
                                Text("₩")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Color(hex: "6B5A65"))
                            }
                        )
                        .keyboardType(.numberPad)
                        ConCafeFormEditor(
                            label: "설명",
                            text: Binding(
                                get: { viewModel.uiState.description },
                                set: { viewModel.onAction(.changeDescription($0)) }
                            )
                        )
                        categorySection
                        Toggle(
                            "재고 있음",
                            isOn: Binding(
                                get: { viewModel.uiState.isInStock },
                                set: { viewModel.onAction(.toggleStock($0)) }
                            )
                        )
                    }
                }
                .padding(16)
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                bottomSaveBar(keyboardBottomInset: keyboardBottomInset)
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillChangeFrameNotification)) { notification in
                keyboardOverlap = resolveKeyboardOverlap(notification: notification)
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
                keyboardOverlap = 0
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
        }
        .navigationTitle(viewModel.uiState.screenTitle)
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
                if let imageUrl = saveImageToTemporaryFile(image) {
                    viewModel.onAction(.selectPhoto(imageUrl))
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

    private func bottomSaveBar(keyboardBottomInset: CGFloat) -> some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: "FFD1DC").opacity(0.2))
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
                        Text(viewModel.uiState.saveButtonLabel)
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                }
                .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(hex: "FFD1DC"))
            .foregroundStyle(Color(hex: "2B2330"))
            .disabled(viewModel.uiState.isSaving || viewModel.uiState.isLoading)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color.white.opacity(0.92))
        }
        .padding(.bottom, keyboardBottomInset)
    }

    private var categorySection: some View {
        let categoryIds = ["drink", "food", "dessert", "goods"]
        return VStack(alignment: .leading, spacing: 10) {
            Text("카테고리")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
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
            .foregroundStyle(isSelected ? Color(hex: "2B2330") : Color(hex: "6E6169"))
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(isSelected ? Color(hex: "FFD1DC").opacity(0.2) : Color(hex: "F8F5F6"))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(
                        isSelected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.3),
                        lineWidth: isSelected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private func categoryLabel(categoryId: String) -> String {
        switch categoryId {
        case "food":
            return "음식"
        case "dessert":
            return "디저트"
        case "goods":
            return "굿즈"
        default:
            return "음료"
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
                                colors: [Color(hex: "FFD8E6"), Color(hex: "FFE5EE")],
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
                        .stroke(Color(hex: "FFD1DC"), lineWidth: 1.5)
                )
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private var loadingPhotoPlaceholder: some View {
        VStack(spacing: 8) {
            ProgressView()
                .tint(Color(hex: "9C7A88"))
            Text("이미지 로딩 중")
                .font(.caption)
                .foregroundStyle(Color(hex: "8F848F"))
        }
    }

    private var photoUploadPlaceholder: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.badge.plus")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(Color(hex: "8B5164"))
            Text("항목 사진 업로드")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color(hex: "5A4954"))
            Text("JPG, PNG 최대 5MB")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
        }
    }

    private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
        saveCompressedImageToTemporaryFile(image)
    }

    private func resolveKeyboardOverlap(notification: Notification) -> CGFloat {
        let userInfo = notification.userInfo
        let keyboardFrame = (userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue
        let screenHeight = UIScreen.main.bounds.height
        let overlap = max(0, screenHeight - (keyboardFrame?.minY ?? screenHeight))
        return overlap
    }
}

struct MenuGoodsEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            MenuGoodsEditView(cafeId: "cafe-1", itemId: nil, onNavigationAction: { _ in })
        }
    }
}
