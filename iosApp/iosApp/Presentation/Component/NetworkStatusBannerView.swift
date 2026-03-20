//
//  NetworkStatusBannerView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/20.
//

import SwiftUI

struct NetworkStatusBannerView: View {
    let message: String

    let isConnected: Bool

    var body: some View {
        Text(message)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isConnected ? Color(hex: "166534") : Color(hex: "991B1B"))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(isConnected ? Color(hex: "DCFCE7") : Color(hex: "FEE2E2"))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .accessibilityLabel(message)
    }
}

struct NetworkStatusBannerView_Previews: PreviewProvider {
    static var previews: some View {
        NetworkStatusBannerView(message: "Test", isConnected: false)
    }
}
