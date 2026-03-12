//
//  ScheduleAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum ScheduleAction {
    case clickBack
    case clickMore
    case clickCalendar
    case selectDay(id: String)
    case clickEditDay(id: String)
    case clickSave
    case dismissInfoMessage
}
