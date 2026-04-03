//
//  NotificationQuietHoursOption.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

enum NotificationQuietHoursOption: String, CaseIterable, Identifiable {
    case off
    case night
    case allDay

    var id: String { rawValue }

    var title: String {
        switch self {
        case .off:
            return String(localized: String.LocalizationValue("notification_quiet_off_title"), table: "Localizable")
        case .night:
            return String(localized: String.LocalizationValue("notification_quiet_night_title"), table: "Localizable")
        case .allDay:
            return String(localized: String.LocalizationValue("notification_quiet_all_day_title"), table: "Localizable")
        }
    }

    var description: String {
        switch self {
        case .off:
            return String(localized: String.LocalizationValue("notification_quiet_off_desc"), table: "Localizable")
        case .night:
            return String(localized: String.LocalizationValue("notification_quiet_night_desc"), table: "Localizable")
        case .allDay:
            return String(localized: String.LocalizationValue("notification_quiet_all_day_desc"), table: "Localizable")
        }
    }
}
