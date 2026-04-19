//
//  Color.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import SwiftUI
import UIKit

extension Color {
    init(hex: String) {
        let value = Int(hex, radix: 16) ?? 0
        let red = CGFloat((value >> 16) & 0xFF) / 255.0
        let green = CGFloat((value >> 8) & 0xFF) / 255.0
        let blue = CGFloat(value & 0xFF) / 255.0

        self.init(
            UIColor { traitCollection in
                if traitCollection.userInterfaceStyle == .dark {
                    let adapted = Self.adaptForDarkMode(red: red, green: green, blue: blue)
                    return UIColor(red: adapted.red, green: adapted.green, blue: adapted.blue, alpha: 1)
                }
                return UIColor(red: red, green: green, blue: blue, alpha: 1)
            }
        )
    }

    private static func adaptForDarkMode(red: CGFloat, green: CGFloat, blue: CGFloat) -> (red: CGFloat, green: CGFloat, blue: CGFloat) {
        let luminance = (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
        let maxChannel = max(red, max(green, blue))
        let minChannel = min(red, min(green, blue))
        let saturation = maxChannel - minChannel

        if saturation < 0.12 {
            if luminance > 0.85 {
                return (0.10, 0.11, 0.13)
            }
            if luminance > 0.65 {
                return blend(red: red, green: green, blue: blue, target: 0, ratio: 0.72)
            }
            if luminance > 0.45 {
                return blend(red: red, green: green, blue: blue, target: 0, ratio: 0.52)
            }
            if luminance < 0.20 {
                return blend(red: red, green: green, blue: blue, target: 1, ratio: 0.38)
            }
            return blend(red: red, green: green, blue: blue, target: 1, ratio: 0.14)
        }
        if luminance > 0.80 {
            return blend(red: red, green: green, blue: blue, target: 0, ratio: 0.65)
        }
        if luminance > 0.60 {
            return blend(red: red, green: green, blue: blue, target: 0, ratio: 0.45)
        }
        if luminance < 0.25 {
            return blend(red: red, green: green, blue: blue, target: 1, ratio: 0.28)
        }
        return blend(red: red, green: green, blue: blue, target: 0, ratio: 0.18)
    }

    private static func blend(
        red: CGFloat,
        green: CGFloat,
        blue: CGFloat,
        target: CGFloat,
        ratio: CGFloat
    ) -> (red: CGFloat, green: CGFloat, blue: CGFloat) {
        let clampedRatio = min(max(ratio, 0), 1)
        let mixedRed = (red * (1 - clampedRatio)) + (target * clampedRatio)
        let mixedGreen = (green * (1 - clampedRatio)) + (target * clampedRatio)
        let mixedBlue = (blue * (1 - clampedRatio)) + (target * clampedRatio)
        return (mixedRed, mixedGreen, mixedBlue)
    }
}
