//
//  CafeMenuView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import UIKit
import Shared

struct CafeMenuView: View {
    let menus: [CafeMenu]

    let goods: [Goods]

    let isLoading: Bool

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        let hasMenu = !menus.isEmpty
        let hasGoods = !goods.isEmpty

        if isLoading && !hasMenu && !hasGoods {
            ShimmerListSkeleton(itemCount: 6, avatarSize: 84, isAvatarCircular: false)
        } else if !hasMenu && !hasGoods {
            emptyCard(String(localized: String.LocalizationValue("cafe_menu_empty"), table: "Localizable"))
        } else {
            VStack(alignment: .leading, spacing: 28) {
                if hasMenu {
                    menuSection
                }
                if hasGoods {
                    goodsSection
                }
            }
        }
    }

    private var menuSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle(String(localized: String.LocalizationValue("menugoods_tab_menu"), table: "Localizable"))
            ForEach(Array(menus.enumerated()), id: \.element.id) { index, menu in
                menuRow(menu)
                    .lazyListImagePrefetch(
                        index: index,
                        imageUrls: menus.map { $0.image },
                        aheadCount: 8,
                        displaySize: .thumbnail
                    )
            }
        }
    }

    private func menuRow(_ menu: CafeMenu) -> some View {
        HStack(spacing: 12) {
            GeometryReader { proxy in
                let imageSize = proxy.size
                let trimmed = menu.image?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                let resolvedUrl = trimmed.isEmpty ? nil : URL(string: trimmed)

                ZStack {
                    LinearGradient(
                        colors: resolvedUrl == nil
                            ? [ConCafeColors.warningContainer, ConCafeColors.warningContainer]
                            : [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    if let resolvedUrl {
                        CachedAsyncImage(url: resolvedUrl, placeholder: EmptyView())
                            .frame(width: imageSize.width, height: imageSize.height)
                            .clipped()
                        LinearGradient(
                            colors: [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .opacity(0.28)
                    }
                }
                .frame(width: imageSize.width, height: imageSize.height)
            }
            .frame(width: 84, height: 84)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            VStack(alignment: .leading, spacing: 6) {
                Text(menu.name)
                    .font(.subheadline.weight(.semibold))
                Text(
                    String(
                        format: String(localized: String.LocalizationValue("cafe_menu_price"), table: "Localizable"),
                        locale: Locale.current,
                        menu.price
                    )
                )
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(ConCafeColors.primary)
                Text(menu.desc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var goodsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle(String(localized: String.LocalizationValue("menugoods_tab_goods"), table: "Localizable"))
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(Array(goods.enumerated()), id: \.element.id) { index, good in
                    goodsTile(good)
                        .lazyListImagePrefetch(
                            index: index,
                            imageUrls: goods.map { $0.image },
                            aheadCount: 8,
                            displaySize: .thumbnail
                        )
                }
            }
        }
    }

    private func goodsTile(_ good: Goods) -> some View {
        let isInStock = good.stock > 0
        let trimmed = good.image?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let imageUrl = trimmed.isEmpty ? nil : URL(string: trimmed)
        return VStack(alignment: .leading, spacing: 0) {
            ZStack {
                LinearGradient(
                    colors: imageUrl == nil
                        ? [ConCafeColors.warningContainer, ConCafeColors.warningContainer]
                        : [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer],
                    startPoint: .top,
                    endPoint: .bottom
                )
                if let imageUrl {
                    CachedAsyncImage(url: imageUrl, placeholder: EmptyView())
                        .clipped()
                }
                if !isInStock {
                    Color.black.opacity(0.5)
                    Text(String(localized: String.LocalizationValue("menugoods_sold_out"), table: "Localizable"))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ConCafeColors.textPrimary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.white)
                        .clipShape(Capsule())
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(good.name)
                    .font(.caption.weight(.semibold))
                    .lineLimit(2)
                HStack {
                    Text(
                        String(
                            format: String(localized: String.LocalizationValue("cafe_menu_price"), table: "Localizable"),
                            locale: Locale.current,
                            good.price
                        )
                    )
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ConCafeColors.primary)
                    Spacer(minLength: 0)
                    if isInStock {
                        HStack(spacing: 2) {
                            Image(systemName: "bag.fill")
                                .font(.system(size: 10))
                                .foregroundStyle(ConCafeColors.success)
                            Text(String(localized: String.LocalizationValue("cafe_goods_in_stock"), table: "Localizable"))
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(ConCafeColors.success)
                        }
                    }
                }
            }
            .padding(.top, 10)
        }
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.headline.weight(.bold))
            .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : ConCafeColors.textPrimary)
    }

    private func emptyCard(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct CafeMenuView_Previews: PreviewProvider {
    static var previews: some View {
        CafeMenuView(menus: [], goods: [], isLoading: false)
    }
}
