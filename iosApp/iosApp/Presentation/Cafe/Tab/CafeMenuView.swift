//
//  CafeMenuView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
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
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
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
            ForEach(menus, id: \.id) { menu in
                menuRow(menu)
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
                            ? [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")]
                            : [Color(hex: "FFD8E8"), Color(hex: "F5AFCC")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    if let resolvedUrl {
                        CachedAsyncImage(url: resolvedUrl, placeholder: EmptyView())
                            .frame(width: imageSize.width, height: imageSize.height)
                            .clipped()
                        LinearGradient(
                            colors: [Color(hex: "FFD8E8"), Color(hex: "F5AFCC")],
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
                .foregroundStyle(Color(hex: "EF6797"))
                Text(menu.desc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var goodsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle(String(localized: String.LocalizationValue("menugoods_tab_goods"), table: "Localizable"))
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(goods, id: \.id) { good in
                    goodsTile(good)
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
                        ? [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")]
                        : [Color(hex: "FFD8E8"), Color(hex: "F5AFCC")],
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
                        .foregroundStyle(Color(hex: "2B2330"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.white)
                        .clipShape(Capsule())
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .cornerRadiusCompat(
                topLeft: 20,
                topRight: 20,
                bottomLeft: 0,
                bottomRight: 0
            )
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
                    .foregroundStyle(Color(hex: "EF6797"))
                    Spacer(minLength: 0)
                    if isInStock {
                        HStack(spacing: 2) {
                            Image(systemName: "bag.fill")
                                .font(.system(size: 10))
                                .foregroundStyle(Color(hex: "16A34A"))
                            Text(String(localized: String.LocalizationValue("cafe_goods_in_stock"), table: "Localizable"))
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(Color(hex: "16A34A"))
                        }
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.headline.weight(.bold))
    }

    private func emptyCard(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct CafeMenuView_Previews: PreviewProvider {
    static var previews: some View {
        CafeMenuView(menus: [], goods: [], isLoading: false)
    }
}
