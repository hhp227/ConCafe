//
//  CafeNoticeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeNoticeView: View {
    let notices: [Notice]
    
    var body: some View {
        if notices.isEmpty {
            emptyCard("등록된 공지가 없습니다.")
        } else {
            VStack(spacing: 12) {
                ForEach(notices, id: \.id) { notice in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(notice.title)
                                .font(.subheadline.weight(.semibold))
                            Spacer()
                            Text(String(notice.createdAt.prefix(10)))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(notice.content)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "666666"))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
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

struct CafeNoticeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeNoticeView(notices: [])
    }
}
