//
//  SignInComponents.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct SignInLogoSection: View {
    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(hex: "EF6797"), Color(hex: "F7A8C8")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 96, height: 96)
                Image(systemName: "heart.fill")
                    .font(.system(size: 38))
                    .foregroundStyle(.white)
            }
            Text("ConCafe")
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Color(hex: "DA4E84"))
            Text("메이드카페의 모든 것")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.top, 8)
        .frame(maxWidth: .infinity)
    }
}

struct SignInDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle()
                .fill(Color(hex: "E5DEE6"))
                .frame(height: 1)
            Text("또는")
                .font(.footnote)
                .foregroundStyle(.secondary)
            Rectangle()
                .fill(Color(hex: "E5DEE6"))
                .frame(height: 1)
        }
    }
}

struct SignInSocialButton: View {
    let title: String
    
    let icon: String
    
    let background: Color
    
    let foreground: Color
    
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(icon)
                Text(title)
                    .font(.headline)
            }
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}
