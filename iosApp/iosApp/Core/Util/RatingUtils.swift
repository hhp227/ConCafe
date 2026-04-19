//
//  RatingUtils.swift
//  ConCafe
//
//  Created by 홍희표 on 4/19/26.
//

import Foundation

enum RatingUtils {
    private static let posixLocale = Locale(identifier: "en_US_POSIX")

    static func formatOneDecimal(_ rating: Double) -> String {
        String(format: "%.1f", locale: posixLocale, rating)
    }

    static func formatOneDecimal(_ rating: Float) -> String {
        formatOneDecimal(Double(rating))
    }

    static func formatOneDecimalTruncated(_ rating: Double) -> String {
        let truncated = (rating * 10).rounded(.towardZero) / 10
        return formatOneDecimal(truncated)
    }
}
