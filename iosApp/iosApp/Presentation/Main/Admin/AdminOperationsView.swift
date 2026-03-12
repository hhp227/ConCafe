//
//  AdminOperationsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct AdminOperationsView: View {
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

struct AdminOperationsView_Previews: PreviewProvider {
    static var previews: some View {
        AdminOperationsView(title: "운영관리", description: "관리자가 승인, Claim, 신고, 밴 처리를 수행하는 메인 탭입니다.")
    }
}
