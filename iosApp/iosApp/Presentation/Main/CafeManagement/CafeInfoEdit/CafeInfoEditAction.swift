//
//  CafeInfoEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

enum CafeInfoEditAction {
    case clickBack
    case changeCafeName(String)
    case changeCafeDescription(String)
    case changeConceptType(String)
    case changeAddress(String)
    case changeContactNumber(String)
    case changeWeekdayOpen(String)
    case changeWeekdayClose(String)
    case changeWeekendOpen(String)
    case changeWeekendClose(String)
    case clickRepresentativeImage
    case selectRepresentativeImage(String)
    case clickAddGalleryImage
    case addGalleryImage(String)
    case removeGalleryImage(Int)
    case clickPinLocation
    case setPinnedLocation(latitude: Double, longitude: Double)
    case clickManageExceptionDates
    case dismissImageRequiredAlert
    case clickSave
    case dismissInfoMessage
}
