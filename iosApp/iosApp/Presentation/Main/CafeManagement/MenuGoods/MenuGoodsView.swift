//
//  MenuGoodsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI

struct MenuGoodsView: View {
    let cafeId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: MenuGoodsViewModel

    var body: some View {
        MenuGoodsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle("메뉴&굿즈 관리")
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
            "항목 삭제",
            isPresented: Binding(
                get: { viewModel.uiState.pendingDeleteItem != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.cancelDeleteItem)
                    }
                }
            ),
            presenting: viewModel.uiState.pendingDeleteItem
        ) { _ in
            Button("취소", role: .cancel) {
                viewModel.onAction(.cancelDeleteItem)
            }
            Button("삭제", role: .destructive) {
                viewModel.onAction(.confirmDeleteItem)
            }
        } message: { item in
            Text("'\(item.name)' 항목을 삭제하시겠습니까? 삭제 후 되돌릴 수 없습니다.")
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
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(spacing: 16) {
                    cafeContextCard
                    collectionTabs
                    if uiState.isSearchVisible {
                        searchField
                    }
                    categoryChips
                    if let infoMessage = uiState.infoMessage {
                        infoBanner(message: infoMessage)
                    }
                    if uiState.isLoading {
                        loadingCard
                    } else if uiState.filteredVisibleItems.isEmpty {
                        emptyStateCard
                    } else {
                        VStack(spacing: 14) {
                            ForEach(uiState.filteredVisibleItems, id: \.id) { item in
                                manageItemCard(item: item)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 104)
            }
            .background(
                LinearGradient(
                    colors: [Color(hex: "FFF8FB"), Color(hex: "FFF2F6"), Color(hex: "FFFCFD")],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            Button {
                onAction(.clickAddNewItem)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus")
                    Text("새 항목 추가")
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
            .padding(.trailing, 20)
            .padding(.bottom, 24)
        }
    }

    private var cafeContextCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(uiState.cafeName.isEmpty ? "카페 판매 항목" : uiState.cafeName)
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)
            Text("메뉴와 굿즈 판매 상태를 한 화면에서 관리합니다.")
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
            collectionTabButton(title: "메뉴", tab: .menu)
            collectionTabButton(title: "굿즈", tab: .goods)
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
                "항목명, 카테고리, 키워드 검색",
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
                    let selected = chip.id == uiState.selectedCategoryId || (chip.id == nil && uiState.selectedCategoryId == nil)
                    
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
            Text(uiState.searchQuery.isEmpty ? "등록된 항목이 없습니다." : "검색 결과가 없습니다.")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(
                uiState.searchQuery.isEmpty
                    ? "새 메뉴나 굿즈를 등록하면 이 목록에 표시됩니다."
                    : "검색어 또는 카테고리를 바꿔 다시 확인해보세요."
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

    private func manageItemCard(item: MenuGoodsUiState.ManageItem) -> some View {
        HStack(alignment: .top, spacing: 14) {
            itemThumbnail(item: item)
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.name)
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color(hex: "2B2330"))
                            .lineLimit(1)
                        Text(item.priceText)
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
                Text(item.badgeLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "B64A79"))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color(hex: "FCE7EF"))
                    .clipShape(Capsule())
                Text(item.description)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7B6B75"))
                    .lineLimit(2)
                if let inventoryLabel = item.inventoryLabel {
                    Text(inventoryLabel)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Color(hex: "8B7A84"))
                }
                Divider()
                    .overlay(Color(hex: "F4E7EE"))
                HStack {
                    Text(item.availabilityLabel)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(item.isAvailable ? Color(hex: "3B7B5A") : Color(hex: "8A7A82"))
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { item.isAvailable },
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

    private func itemThumbnail(item: MenuGoodsUiState.ManageItem) -> some View {
        let isMenu = uiState.selectedCollection == .menu
        let colors: [Color]

        if isMenu {
            colors = item.isAvailable
                ? [Color(hex: "FFE0EA"), Color(hex: "FAB6D0")]
                : [Color(hex: "F1E2EA"), Color(hex: "D7C1CE")]
        } else {
            colors = item.isAvailable
                ? [Color(hex: "FFEBCB"), Color(hex: "FFD7A1")]
                : [Color(hex: "E7E1DA"), Color(hex: "CBC0B2")]
        }
        return ZStack {
            LinearGradient(
                colors: colors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            VStack(spacing: 6) {
                Image(systemName: isMenu ? "storefront" : "shippingbox")
                    .foregroundStyle(Color(hex: "704A5F"))
                Text(String(item.name.prefix(1)))
                    .font(.title.weight(.bold))
                    .foregroundStyle(Color(hex: "704A5F"))
            }
        }
        .frame(width: 96, height: 108)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
