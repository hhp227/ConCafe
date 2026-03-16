//
//  ExternalLinkViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine

@MainActor
final class ExternalLinkViewModel: ObservableObject {
    @Published private(set) var uiState: ExternalLinkUiState

    let event = PassthroughSubject<ExternalLinkEvent, Never>()

    func onAction(_ action: ExternalLinkAction) {
        switch action {
        case .tapBack:
            event.send(.navigateBack)
        }
    }
    
    init(title: String, url: String) {
        self.uiState = ExternalLinkUiState(title: title, url: url)
    }
}

