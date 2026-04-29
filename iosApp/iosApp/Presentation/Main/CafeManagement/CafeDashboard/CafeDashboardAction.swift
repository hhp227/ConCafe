//
//  CafeDashboardAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation

enum CafeDashboardAction {
    case clickBack
    case clickShortcut(CafeDashboardShortcut)
    case clickCreateBanner
    case dismissExternalLinkSheet
    case changeExternalLinkTitle(String)
    case changeExternalLinkUrl(String)
    case submitExternalLink
    case clickExternalLinkItem(String)
    case clickEditExternalLink(String)
    case clickDeleteExternalLink(String)
    case clickCastSchedule(String)
    case clickDeleteCast
    case confirmDeleteCast
    case dismissDeleteCastDialog
    case clickApproveCastClaim(String)
    case clickRejectCastClaim(String)
    case clickLoadMoreCasts
    case dismissInfoMessage
    case changeSocialMediaInstagram(String)
    case changeSocialMediaTwitter(String)
    case changeSocialMediaTiktok(String)
    case changeSocialMediaYoutube(String)
    case submitSocialMedia
    case dismissSocialMediaSheet
    case dismissReservationSheet
    case changeReservationUrl(String)
    case submitReservation
    case clickTableCountMetric
    case dismissTableCountSheet
    case changeCurrentTableCount(String)
    case changeTotalTableCount(String)
    case submitTableCounts
}
