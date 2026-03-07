//
//  FanManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct FanManagementView: View {
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

struct FanManagementView_Previews: PreviewProvider {
    static var previews: some View {
        FanManagementView(title: "팬관리", description: "캐스트가 팔로워, 출근 일정, 팬 대상 공지를 관리하는 메인 탭입니다.")
    }
}
