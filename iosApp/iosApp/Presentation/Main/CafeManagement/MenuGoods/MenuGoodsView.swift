//
//  MenuGoodsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import Foundation
import Shared

struct MenuGoodsView: View {
    let cafeId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: MenuGoodsViewModel

    var body: some View {
        MenuGoodsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("menugoods_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    viewModel.onAction(.clickSearch)
                } label: {
                    Image(systemName: "magnifyingglass")
                }
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToEdit(let cafeId, let itemId):
                onNavigationAction(.navigateToMenuGoodsEdit(cafeId: cafeId, itemId: itemId))
            }
        }
        .alert(
            String(localized: String.LocalizationValue("menugoods_delete_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.pendingDeleteItemId != nil },
                set: { _ in }
            )
        ) {
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) {
                viewModel.onAction(.cancelDeleteItem)
            }
            Button(String(localized: String.LocalizationValue("menugoods_delete_confirm"), table: "Localizable"), role: .destructive) {
                viewModel.onAction(.confirmDeleteItem)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("menugoods_delete_message"), table: "Localizable"))
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: MenuGoodsViewModel(cafeId: cafeId))
    }
}

private struct MenuGoodsContentView: View {
    let uiState: MenuGoodsUiState

    let onAction: (MenuGoodsAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                cafeContextCard
                collectionTabs
                if uiState.isSearchVisible {
                    searchField
                }
                categoryChips
                if let infoMessage = uiState.infoMessage {
                    infoBanner(
                        message: {
                            switch infoMessage {
                            case "menugoods_info_load_failed",
                                 "menugoods_info_availability_save_failed",
                                 "menugoods_info_delete_success",
                                 "menugoods_info_delete_failed":
                                return String(localized: String.LocalizationValue(infoMessage), table: "Localizable")
                            default:
                                return infoMessage
                            }
                        }()
                    )
                }
                if uiState.isLoading {
                    loadingCard
                } else if uiState.selectedCollection == .menu && uiState.filteredMenuItems.isEmpty {
                    emptyStateCard
                } else if uiState.selectedCollection == .goods && uiState.filteredGoodsItems.isEmpty {
                    emptyStateCard
                } else {
                    VStack(spacing: 14) {
                        if uiState.selectedCollection == .menu {
                            ForEach(uiState.filteredMenuItems, id: \.id) { item in
                                menuItemCard(item: item)
                            }
                        } else {
                            ForEach(uiState.filteredGoodsItems, id: \.id) { item in
                                goodsItemCard(item: item)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF8FB"), Color(hex: "FFF2F6"), Color(hex: "FFFCFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .safeAreaInset(edge: .bottom, spacing: 0) {
            HStack {
                Spacer()
                Button {
                    onAction(.clickAddNewItem)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                        Text(String(localized: String.LocalizationValue("menugoods_add_new_item"), table: "Localizable"))
                            .font(.subheadline.weight(.bold))
                    }
                    .foregroundStyle(Color(hex: "2B2330"))
                    .padding(.horizontal, 18)
                    .padding(.vertical, 14)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(Capsule())
                    .shadow(color: Color(hex: "FFD1DC").opacity(0.45), radius: 12, x: 0, y: 6)
                }
                .buttonStyle(.plain)
            }
            .padding(.trailing, 20)
            .padding(.top, 12)
            .padding(.bottom, 8)
        }
    }

    private var cafeContextCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(uiState.cafeName.isEmpty ? String(localized: String.LocalizationValue("menugoods_context_default_title"), table: "Localizable") : uiState.cafeName)
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)
            Text(String(localized: String.LocalizationValue("menugoods_context_subtitle"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.88))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color(hex: "351B42"), Color(hex: "7B3F68"), Color(hex: "F28EB5")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var collectionTabs: some View {
        HStack(spacing: 8) {
            collectionTabButton(title: String(localized: String.LocalizationValue("menugoods_tab_menu"), table: "Localizable"), tab: .menu)
            collectionTabButton(title: String(localized: String.LocalizationValue("menugoods_tab_goods"), table: "Localizable"), tab: .goods)
        }
        .padding(4)
        .background(Color(hex: "FFD1DC").opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func collectionTabButton(title: String, tab: MenuGoodsUiState.CollectionTab) -> some View {
        let selected = uiState.selectedCollection == tab
        return Button {
            onAction(.selectCollection(tab))
        } label: {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(selected ? Color(hex: "2B2330") : Color(hex: "7A6671"))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(selected ? Color(hex: "FFD1DC") : .clear)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color(hex: "9A7D8E"))
            TextField(
                String(localized: String.LocalizationValue("menugoods_search_placeholder"), table: "Localizable"),
                text: Binding(
                    get: { uiState.searchQuery },
                    set: { onAction(.changeSearchQuery($0)) }
                )
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "F1D9E4"), lineWidth: 1)
        )
    }

    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(uiState.visibleCategories, id: \.self) { chip in
                    let selected = chip.id == uiState.selectedCategoryId
                    
                    Button {
                        onAction(.selectCategory(chip.id))
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: categorySymbolName(chip.iconKey))
                                .font(.subheadline)
                            Text(chip.label)
                                .font(.subheadline.weight(.semibold))
                        }
                        .foregroundStyle(selected ? Color(hex: "2B2330") : Color(hex: "6F5E68"))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(selected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.2))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "6B5320"))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(hex: "FFF6D7"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
        )
    }

    private var loadingCard: some View {
        ProgressView()
            .tint(Color(hex: "EF6797"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 48)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Color(hex: "F0E2E9"), lineWidth: 1)
            )
    }

    private var emptyStateCard: some View {
        VStack(spacing: 10) {
            Image(systemName: "shippingbox")
                .font(.title2.weight(.bold))
                .foregroundStyle(Color(hex: "EF6797"))
                .frame(width: 44, height: 44)
                .background(Color(hex: "FCE7EF"))
                .clipShape(Circle())
            Text(uiState.searchQuery.isEmpty ? String(localized: String.LocalizationValue("menugoods_empty_default_title"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_empty_search_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(
                uiState.searchQuery.isEmpty
                    ? String(localized: String.LocalizationValue("menugoods_empty_default_desc"), table: "Localizable")
                    : String(localized: String.LocalizationValue("menugoods_empty_search_desc"), table: "Localizable")
            )
            .font(.subheadline)
            .foregroundStyle(Color(hex: "7B6B75"))
            .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 28)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "F0E2E9"), lineWidth: 1)
        )
    }

    private func menuItemCard(item: CafeMenu) -> some View {
        let isAvailable = uiState.isMenuAvailable(item)
        let categoryLabel = uiState.menuCategoryLabel(item)
        return HStack(alignment: .top, spacing: 14) {
            itemThumbnail(
                name: item.name,
                imageUrl: item.image,
                isAvailable: isAvailable,
                isMenu: true
            )
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.name)
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color(hex: "2B2330"))
                            .lineLimit(1)
                        Text(formatPrice(item.price))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                    Spacer(minLength: 8)
                    HStack(spacing: 0) {
                        Button {
                            onAction(.clickEditItem(item.id))
                        } label: {
                            Image(systemName: "square.and.pencil")
                                .foregroundStyle(Color(hex: "7A6671"))
                                .frame(width: 36, height: 36)
                        }
                        .buttonStyle(.plain)
                        Button {
                            onAction(.clickDeleteItem(item.id))
                        } label: {
                            Image(systemName: "trash")
                                .foregroundStyle(Color(hex: "D96B7A"))
                                .frame(width: 36, height: 36)
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text(categoryLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "B64A79"))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color(hex: "FCE7EF"))
                    .clipShape(Capsule())
                Text(item.desc)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7B6B75"))
                    .lineLimit(2)
                Divider()
                    .overlay(Color(hex: "F4E7EE"))
                HStack {
                    Text(isAvailable ? String(localized: String.LocalizationValue("menugoods_available"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_sold_out"), table: "Localizable"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(isAvailable ? Color(hex: "3B7B5A") : Color(hex: "8A7A82"))
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { isAvailable },
                        set: { _ in onAction(.toggleItemAvailability(item.id)) }
                    ))
                    .labelsHidden()
                    .tint(Color(hex: "FFD1DC"))
                }
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "F0E2E9"), lineWidth: 1)
        )
    }

    private func goodsItemCard(item: Goods) -> some View {
        let isAvailable = uiState.isGoodsAvailable(item)
        let categoryLabel = uiState.goodsCategoryLabel(item)
        return HStack(alignment: .top, spacing: 14) {
            itemThumbnail(
                name: item.name,
                imageUrl: item.image,
                isAvailable: isAvailable,
                isMenu: false
            )
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.name)
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color(hex: "2B2330"))
                            .lineLimit(1)
                        Text(formatPrice(item.price))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                    Spacer(minLength: 8)
                    HStack(spacing: 0) {
                        Button {
                            onAction(.clickEditItem(item.id))
                        } label: {
                            Image(systemName: "square.and.pencil")
                                .foregroundStyle(Color(hex: "7A6671"))
                                .frame(width: 36, height: 36)
                        }
                        .buttonStyle(.plain)
                        Button {
                            onAction(.clickDeleteItem(item.id))
                        } label: {
                            Image(systemName: "trash")
                                .foregroundStyle(Color(hex: "D96B7A"))
                                .frame(width: 36, height: 36)
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text(categoryLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "B64A79"))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color(hex: "FCE7EF"))
                    .clipShape(Capsule())
                Text(String(localized: String.LocalizationValue("menugoods_goods_desc"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7B6B75"))
                    .lineLimit(2)
                Text(
                    String(
                        format: String(localized: String.LocalizationValue("menugoods_stock"), table: "Localizable"),
                        locale: Locale.current,
                        item.stock
                    )
                )
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Color(hex: "8B7A84"))
                Divider()
                    .overlay(Color(hex: "F4E7EE"))
                HStack {
                    Text(isAvailable ? String(localized: String.LocalizationValue("menugoods_available"), table: "Localizable") : String(localized: String.LocalizationValue("menugoods_sold_out"), table: "Localizable"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(isAvailable ? Color(hex: "3B7B5A") : Color(hex: "8A7A82"))
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { isAvailable },
                        set: { _ in onAction(.toggleItemAvailability(item.id)) }
                    ))
                    .labelsHidden()
                    .tint(Color(hex: "FFD1DC"))
                }
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "F0E2E9"), lineWidth: 1)
        )
    }

    private func itemThumbnail(name: String, imageUrl: String?, isAvailable: Bool, isMenu: Bool) -> some View {
        let colors: [Color]

        if isMenu {
            colors = isAvailable
                ? [Color(hex: "FFE0EA"), Color(hex: "FAB6D0")]
                : [Color(hex: "F1E2EA"), Color(hex: "D7C1CE")]
        } else {
            colors = isAvailable
                ? [Color(hex: "FFEBCB"), Color(hex: "FFD7A1")]
                : [Color(hex: "E7E1DA"), Color(hex: "CBC0B2")]
        }
        return GeometryReader { proxy in
            let imageSize = proxy.size
            let trimmed = imageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let resolvedUrl = trimmed.isEmpty ? nil : URL(string: trimmed)
            ZStack {
                if let resolvedUrl {
                    CachedAsyncImage(
                        url: resolvedUrl,
                        placeholder: EmptyView()
                    )
                    .frame(width: imageSize.width, height: imageSize.height)
                    .clipped()
                }
                LinearGradient(
                    colors: colors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .opacity(resolvedUrl == nil ? 1 : 0.28)
                VStack(spacing: 6) {
                    Image(systemName: isMenu ? "storefront" : "shippingbox")
                        .foregroundStyle(Color(hex: "704A5F"))
                    Text(String(name.prefix(1)))
                        .font(.title.weight(.bold))
                        .foregroundStyle(Color(hex: "704A5F"))
                }
            }
        }
        .frame(width: 96, height: 108)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func formatPrice(_ price: Int32) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        let number = NSNumber(value: price)
        let formatted = formatter.string(from: number) ?? "\(price)"
        return "KRW \(formatted)"
    }

    private func categorySymbolName(_ iconKey: String) -> String {
        switch iconKey {
        case "food":
            return "fork.knife"
        case "drink":
            return "cup.and.saucer.fill"
        case "dessert":
            return "star.fill"
        case "goods":
            return "shippingbox"
        default:
            return "sparkles"
        }
    }
}

struct MenuGoodsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            MenuGoodsView(cafeId: "cafe-1", onNavigationAction: { _ in })
        }
    }
}
