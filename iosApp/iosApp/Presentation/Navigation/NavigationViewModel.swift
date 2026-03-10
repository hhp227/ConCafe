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
        case .navigateToCafeInfoEdit(let id):
            event.send(.navigateTo(.cafeInfoEdit(param: id)))
        case .navigateToSignIn:
            event.send(.navigateTo(.signIn))
        case .navigateToSignUp:
            event.send(.navigateTo(.signUp))
        case .navigateToNotification:
            event.send(.navigateTo(.notification))
        case .navigateToSettings:
            event.send(.navigateTo(.settings))
        case .navigateBack:
            event.send(.navigateBack)
        }
    }
}
