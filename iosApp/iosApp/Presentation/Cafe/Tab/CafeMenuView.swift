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
    
    var body: some View {
        if menus.isEmpty {
            emptyCard("등록된 메뉴가 없습니다.")
        } else {
            VStack(spacing: 12) {
                ForEach(menus, id: \.id) { menu in
                    HStack(spacing: 12) {
                        GeometryReader { proxy in
                            let imageSize = proxy.size
                            let trimmed = menu.image?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
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
                                    colors: resolvedUrl == nil ? [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")] : [Color(hex: "FFD8E8"), Color(hex: "F5AFCC")],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                                .opacity(resolvedUrl == nil ? 1 : 0.28)
                            }
                            .frame(width: imageSize.width, height: imageSize.height)
                        }
                        .frame(width: 84, height: 84)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        VStack(alignment: .leading, spacing: 6) {
                            Text(menu.name)
                                .font(.subheadline.weight(.semibold))
                            Text("\(menu.price)원")
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
            }
        }
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
        CafeMenuView(menus: [])
    }
}
