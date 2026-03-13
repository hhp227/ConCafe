//
//  NoticeEventAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum NoticeEventAction {
    case clickBack
    case selectTab(NoticeEventTab)
    case changeQuery(String)
    case loadMoreNotices
    case loadMoreEvents
    case clickRegister
    case clickMoreEvents
    case clickEditNotice(String)
    case clickDeleteNotice(String)
    case clickEventMenu(String)
    case dismissFormSheet
    case changeFormTitle(String)
    case changeFormContent(String)
    case clickFormImage
    case clickRemoveFormImage
    case changeFormPinned(Bool)
    case clickReserveSchedule
    case clickSubmitForm
    case dismissInfoMessage
}
