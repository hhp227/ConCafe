//
//  CastEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum CastEditAction {
    case clickBack
    case clickProfilePhoto
    case changeCastName(String)
    case changeConceptRole(String)
    case changeBirthday(String)
    case changeIntroduction(String)
    case toggleWorkingDay(CastEditUiState.WorkingDay)
    case clickAddGalleryPhoto
    case clickSave
    case dismissInfoMessage
}
