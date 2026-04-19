//
//  RatingBox.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/29.
//

import SwiftUI

struct RatingBox: View {
    let rating: String

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "star.fill")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(Color(hex: "FFAD30"))
        Text(rating)
                .font(.caption)
                .foregroundStyle(.primary)
        }
        .padding(.horizontal, 7)
        .padding(.vertical, 2)
        .background(
            Color(hex: "FEE3E9"),
            in: Capsule()
        )
    }
}

struct RatingBox_Previews: PreviewProvider {
    static var previews: some View {
        RatingBox(rating: "4.7")
    }
}
