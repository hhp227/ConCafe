//
//  CafeCastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeCastView: View {
    let maids: [CafeDetailCast]
    
    let onAction: (CafeAction) -> Void
    
    var body: some View {
        if maids.isEmpty {
            emptyCard("등록된 메이드가 없습니다.")
        } else {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(maids, id: \.cast.id) { maid in
                    VStack(alignment: .leading, spacing: 0) {
                        LinearGradient(
                            colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 160)
                        .overlay(alignment: .topTrailing) {
                            HStack(spacing: 6) {
                                if maid.isWorking {
                                    Text("출근중")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.green)
                                        .clipShape(Capsule())
                                }
                                Text(maid.cast.conceptRole.uppercased())
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(Color.white.opacity(0.9))
                            }
                            .padding(12)
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(maid.cast.name)
                                .font(.subheadline.weight(.semibold))
                            Text(maid.cast.description)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        .padding(12)
                    }
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .onTapGesture {
                        onAction(.maidTapped(id: maid.cast.id))
                    }
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

struct CafeCastView_Previews: PreviewProvider {
    static var previews: some View {
        CafeCastView(maids: [], onAction: { _ in })
    }
}
