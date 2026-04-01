//
//  PhoneUtils.swift
//  ConCafe
//

import Foundation

func formatKoreanPhoneNumber(_ input: String) -> String {
    let digits = String(input.filter { $0.isNumber }.prefix(11))

    if digits.count <= 3 { return digits }
    if digits.count <= 7 { return "\(digits.prefix(3))-\(digits.dropFirst(3))" }
    return "\(digits.prefix(3))-\(digits.dropFirst(3).prefix(4))-\(digits.dropFirst(7))"
}
