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
        case .navigateToCastDetail(let id):
            event.send(.navigateTo(.castDetail(param: id)))
        case .navigateToCafeDetail(let id):
            event.send(.navigateTo(.cafeDetail(param: id)))
        case .navigateToNotification:
            event.send(.navigateTo(.notification))
        }
    }
}
