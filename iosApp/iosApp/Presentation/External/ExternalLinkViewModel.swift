//
//  ExternalLinkViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine

@MainActor
final class ExternalLinkViewModel: ObservableObject {
    @Published private(set) var uiState: ExternalLinkUiState

    let event = PassthroughSubject<ExternalLinkEvent, Never>()

    init(title: String, url: String) {
        self.uiState = ExternalLinkUiState(title: title, url: url)
    }

    func onAction(_ action: ExternalLinkAction) {
        switch action {
        case .tapBack:
            event.send(.navigateBack)
        }
    }
}
