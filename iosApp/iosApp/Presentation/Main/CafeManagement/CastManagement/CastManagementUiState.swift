//
//  CastManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum CastScheduleViewMode: String, CaseIterable {
    case week = "cast_management_view_week"
    case month = "cast_management_view_month"
}

struct CastManagementUiState {
    var isLoading: Bool = true
    var cafeName: String = ""
    var viewMode: CastScheduleViewMode = .week
    var periodStart: Date? = nil
    var periodEnd: Date? = nil
    var weekColumns: [WeekColumn] = []
    var monthOffset: Int = 0
    var monthCells: [MonthCell] = []
    var errorMessage: String?

    struct WeekColumn: Identifiable {
        let id: String
        let dayLabel: String
        let dateLabel: String
        let castNames: [String]
    }

    struct MonthCell: Identifiable {
        let id: String
        let dayNumber: Int
        let castNames: [String]
    }
}
