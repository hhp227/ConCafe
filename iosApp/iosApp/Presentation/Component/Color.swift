//
//  Color.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import SwiftUI

extension Color {
    init(hex: String) {
        let value = Int(hex, radix: 16) ?? 0
        let red = Double((value >> 16) & 0xFF) / 255.0
        let green = Double((value >> 8) & 0xFF) / 255.0
        let blue = Double(value & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }
}
