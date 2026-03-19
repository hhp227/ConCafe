//
//  ScheduleAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

enum ScheduleAction {
    case clickBack
    case clickMore
    case clickCalendar
    case selectDay(id: String)
    case clickEditDay(id: String)
    case dismissEditSheet
    case changeEditStatus(CastScheduleStatus)
    case changeEditStartTime(String)
    case changeEditEndTime(String)
    case submitEditDay
    case clickSave
    case dismissInfoMessage
}
