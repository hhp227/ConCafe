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
            Image("maid_logo")
                .resizable()
                .scaledToFill()
                .frame(width: 96, height: 96)
                .clipShape(Circle())
            ConCafeLogo(color: Color(hex: "DA4E84"))
            Text(String(localized: String.LocalizationValue("signin_logo_subtitle"), table: "Localizable"))
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
            Text(String(localized: String.LocalizationValue("signin_divider_or"), table: "Localizable"))
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
    
    let outlined: Bool
    
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)

                Text(title)
                    .font(.system(size: 16, weight: .medium))
            }
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(background)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(outlined ? Color(hex: "E4DDE5") : Color.clear, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}

