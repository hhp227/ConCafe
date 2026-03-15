//
//  NavigationViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation
import Combine

@MainActor
final class NavigationViewModel: ObservableObject {
    let event = PassthroughSubject<NavigationEvent, Never>()

    func onAction(_ action: NavigationAction) {
        switch action {
        case .navigateToMain(let tab):
            event.send(.navigateTo(.main(initialTab: tab)))
        case .navigateToCast(let id):
            event.send(.navigateTo(.cast(param: id)))
        case .navigateToCafe(let id):
            event.send(.navigateTo(.cafe(param: id)))
        case .navigateToCafeDashboard(let id):
            event.send(.navigateTo(.cafeDashboard(param: id)))
        case .navigateToBanner(let cafeId):
            event.send(.navigateTo(.banner(cafeId: cafeId)))
        case .navigateToBannerEdit(let cafeId):
            event.send(.navigateTo(.bannerEdit(cafeId: cafeId)))
        case .navigateToExternalLink(let title, let url):
            event.send(.navigateTo(.externalLink(title: title, url: url)))
        case .navigateToCafeInfoEdit(let id, let isRegistrationMode):
            event.send(.navigateTo(.cafeInfoEdit(param: id, isRegistrationMode: isRegistrationMode)))
        case .navigateToNoticeEvent(let id):
            event.send(.navigateTo(.noticeEvent(param: id)))
        case .navigateToCastEdit(let cafeId, let castId):
            event.send(.navigateTo(.castEdit(cafeId: cafeId, castId: castId)))
        case .navigateToSchedule(let castId):
            event.send(.navigateTo(.schedule(castId: castId)))
        case .navigateToMenuGoods(let id):
            event.send(.navigateTo(.menuGoods(param: id)))
        case .navigateToMenuGoodsEdit(let cafeId, let itemId):
            event.send(.navigateTo(.menuGoodsEdit(cafeId: cafeId, itemId: itemId)))
        case .navigateToReviewEdit(let cafeId):
            event.send(.navigateTo(.reviewEdit(cafeId: cafeId)))
        case .navigateToSignIn:
            event.send(.navigateTo(.signIn))
        case .navigateToSignUp:
            event.send(.navigateTo(.signUp))
        case .navigateToNotification:
            event.send(.navigateTo(.notification))
        case .navigateToSettings:
            event.send(.navigateTo(.settings))
        case .navigateToNotificationSettings:
            event.send(.navigateTo(.notificationSettings))
        case .navigateToAccountSettings:
            event.send(.navigateTo(.accountSettings))
        case .navigateToChangePassword:
            event.send(.navigateTo(.changePassword))
        case .navigateBack:
            event.send(.navigateBack)
        }
    }
}
