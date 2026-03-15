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
            return "즉시 받기"
        case .night:
            return "밤 시간만 조용히"
        case .allDay:
            return "요약만 받기"
        }
    }

    var description: String {
        switch self {
        case .off:
            return "중요 알림을 포함해 들어오는 즉시 알려드려요."
        case .night:
            return "밤 11시부터 오전 8시까지는 조용히 보관해요."
        case .allDay:
            return "하루 동안 모아 저녁 시간에 한 번 정리해드려요."
        }
    }
}
