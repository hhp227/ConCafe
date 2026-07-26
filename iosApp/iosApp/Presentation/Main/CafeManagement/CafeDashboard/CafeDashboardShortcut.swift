//
//  CafeDashboardShortcut.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation

enum CafeDashboardShortcut: String, CaseIterable, Identifiable {
    case castManagement
    case castSchedule
    case eventManagement
    case cafeSettings
    case menuGoods
    case homeBanner
    case externalLinks
    case socialMedia
    case reservation

    var id: String { rawValue }

    var title: String {
        switch self {
        case .castManagement: return "dashboard_shortcut_cast_management"
        case .castSchedule: return "dashboard_shortcut_cast_schedule"
        case .eventManagement: return "dashboard_shortcut_event_management"
        case .cafeSettings: return "dashboard_shortcut_cafe_settings"
        case .menuGoods: return "dashboard_shortcut_menu_goods"
        case .homeBanner: return "dashboard_shortcut_home_banner"
        case .externalLinks: return "dashboard_shortcut_external_links"
        case .socialMedia: return "dashboard_shortcut_social_media"
        case .reservation: return "dashboard_shortcut_reservation"
        }
    }
}
