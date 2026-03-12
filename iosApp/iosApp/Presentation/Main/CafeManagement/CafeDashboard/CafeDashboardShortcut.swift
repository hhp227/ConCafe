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

    var id: String { rawValue }

    var title: String {
        switch self {
        case .castManagement: return "캐스트 관리"
        case .castSchedule: return "출근표"
        case .eventManagement: return "공지&이벤트"
        case .cafeSettings: return "카페 정보 관리"
        case .menuGoods: return "메뉴&굿즈"
        case .homeBanner: return "홈 배너"
        case .externalLinks: return "외부 링크"
        }
    }
}
