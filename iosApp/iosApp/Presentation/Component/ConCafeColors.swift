//
//  ConCafeColors.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/07.
//

import Foundation
import SwiftUI
import UIKit

/// ConCafe 디자인 시스템 시맨틱 컬러 토큰.
///
/// 브랜드 테마 2종을 지원한다:
/// - 메이드카페(maidCafe, 기본): 핑크 팔레트 (maidLight 값)
/// - 맨즈콘카페(mensConCafe): Purple Dream 팔레트 (light 값)
/// 다크 모드는 브랜드 테마와 무관하게 Purple Dream 다크 값(dark)을 사용한다.
///
/// 화면에서는 hex 하드코딩 대신 반드시 이 토큰을 사용한다.
/// 토큰 이름과 값은 Compose의 ConCafeColors.kt와 1:1로 동일하게 유지한다.
/// 카카오/애플/구글/X/틱톡 등 서드파티 브랜드 색상만 예외로 hex를 직접 사용한다.
enum ConCafeColors {
    static var brandTheme: AppBrandTheme = .maidCafe

    // Brand
    static var primary: Color { themedColor(light: 0x5E548E, dark: 0x9F86C0, maidLight: 0xEF6797) }
    static var onPrimary: Color { themedColor(light: 0xFFFFFF, dark: 0x231942, maidLight: 0xFFFFFF) }
    static var primaryContainer: Color { themedColor(light: 0xEFE9F6, dark: 0x3A2F55, maidLight: 0xFFD1DC) }
    static var onPrimaryContainer: Color { themedColor(light: 0x4A4174, dark: 0xD6C8F0, maidLight: 0x7C3F67) }
    static var secondary: Color { themedColor(light: 0x9F86C0, dark: 0xBE95C4, maidLight: 0xF7A0C1) }
    static var onSecondary: Color { themedColor(light: 0xFFFFFF, dark: 0x2E2347, maidLight: 0xFFFFFF) }
    static var secondaryContainer: Color { themedColor(light: 0xE7DDF3, dark: 0x453963, maidLight: 0xF8C5D7) }
    static var onSecondaryContainer: Color { themedColor(light: 0x514578, dark: 0xDFD3F0, maidLight: 0x8B5164) }
    static var tertiary: Color { themedColor(light: 0xC97BA0, dark: 0xE0B1CB, maidLight: 0xD1436F) }
    static var onTertiary: Color { themedColor(light: 0xFFFFFF, dark: 0x43202F, maidLight: 0xFFFFFF) }
    static var tertiaryContainer: Color { themedColor(light: 0xF9E4EE, dark: 0x4E3040, maidLight: 0xFDE7EF) }
    static var onTertiaryContainer: Color { themedColor(light: 0x8B4A68, dark: 0xF0CCDE, maidLight: 0x9E2E5C) }

    // Neutral
    static var background: Color { themedColor(light: 0xFBFAFD, dark: 0x171225, maidLight: 0xFFFBFD) }
    static var surface: Color { themedColor(light: 0xFFFFFF, dark: 0x221B36, maidLight: 0xFFFFFF) }
    static var surfaceVariant: Color { themedColor(light: 0xF4F1F8, dark: 0x2A2244, maidLight: 0xF8F5F6) }
    static var surfaceTint: Color { themedColor(light: 0xF8F6FB, dark: 0x1D1730, maidLight: 0xFCE6EF) }
    static var textPrimary: Color { themedColor(light: 0x231942, dark: 0xEDE8F5, maidLight: 0x2B2330) }
    static var textSecondary: Color { themedColor(light: 0x6E6590, dark: 0xA79CC4, maidLight: 0x7A707A) }
    static var textMuted: Color { themedColor(light: 0x9C94B3, dark: 0x7A6FA0, maidLight: 0x8F848F) }
    static var outline: Color { themedColor(light: 0xE9E3F2, dark: 0x332A4E, maidLight: 0xE8DFE7) }
    static var outlineStrong: Color { themedColor(light: 0xCFC7E0, dark: 0x453B66, maidLight: 0xB3ACB7) }

    // Status (브랜드 테마 공통)
    static var success: Color { themedColor(light: 0x2E9E5B, dark: 0x5BC98A) }
    static var successContainer: Color { themedColor(light: 0xE4F5EB, dark: 0x1E3B2C) }
    static var error: Color { themedColor(light: 0xE53935, dark: 0xF28B84) }
    static var errorContainer: Color { themedColor(light: 0xFDE9E7, dark: 0x45211E) }
    static var warning: Color { themedColor(light: 0xD9822B, dark: 0xF0BE6B) }
    static var warningContainer: Color { themedColor(light: 0xFBEEDC, dark: 0x43351A) }
    static var info: Color { themedColor(light: 0x4A79E8, dark: 0x8FB0FF) }
    static var infoContainer: Color { themedColor(light: 0xE8F0FF, dark: 0x202C4D) }

    // Gold (랭킹/별점, 브랜드 테마 공통)
    static var gold: Color { themedColor(light: 0xE2A81E, dark: 0xF1D88D) }
    static var goldDeep: Color { themedColor(light: 0x6B5320, dark: 0xE8D290) }
    static var goldContainer: Color { themedColor(light: 0xFFF6D7, dark: 0x46390F) }

    private static func themedColor(light: Int, dark: Int, maidLight: Int? = nil) -> Color {
        Color(
            UIColor { traitCollection in
                let value: Int
                if traitCollection.userInterfaceStyle == .dark {
                    value = dark
                } else if brandTheme == .maidCafe {
                    value = maidLight ?? light
                } else {
                    value = light
                }
                return UIColor(
                    red: CGFloat((value >> 16) & 0xFF) / 255.0,
                    green: CGFloat((value >> 8) & 0xFF) / 255.0,
                    blue: CGFloat(value & 0xFF) / 255.0,
                    alpha: 1
                )
            }
        )
    }
}
