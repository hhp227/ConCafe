//
//  CafeManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct CafeManagementView: View {
    let title: String

    let description: String

    var body: some View {
        VStack(spacing: 12) {
            Text(title)
                .font(.title2)
                .bold()
            Text(description)
                .font(.body)
                .foregroundStyle(Color(hex: "6B6170"))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF7FB"), Color(hex: "FFEDF5")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}

struct CafeManagementView_Previews: PreviewProvider {
    static var previews: some View {
        CafeManagementView(title: "카페관리", description: "카페 운영자가 공지, 이벤트, 메뉴, 캐스트 운영을 관리하는 메인 탭입니다.")
    }
}
