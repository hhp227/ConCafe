//
//  BannerEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

enum BannerEditAction {
    case clickBack
    case clickImagePicker
    case selectImage(String)
    case changeTitle(String)
    case changeSubtitle(String)
    case selectTarget(BannerTargetType)
    case changeTargetValue(String)
    case changeDisplayDays(Int)
    case clickCafeSelector
    case clickTargetSelector
    case changeSelectorQuery(String)
    case selectSelectorItem(String)
    case dismissSelector
    case dismissImageRequiredAlert
    case clickSave
    case dismissInfoMessage
}
