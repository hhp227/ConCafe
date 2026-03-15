//
//  BannerViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation
import Combine

final class BannerViewModel: ObservableObject {
    private let cafeId: String?

    @Published private(set) var uiState = BannerUiState()

    let event = PassthroughSubject<BannerEvent, Never>()

    func onAction(_ action: BannerAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .selectTab(let tab):
            uiState.selectedTab = tab
        case .createBannerTapped:
            event.send(.navigateToBannerEdit(cafeId: cafeId))
        case .editBannerTapped:
            event.send(.showMessage("편집 기능은 아직 연결되지 않았습니다."))
        case .deleteBannerTapped:
            event.send(.showMessage("삭제 기능은 아직 연결되지 않았습니다."))
        }
    }

    init(cafeId: String? = nil) {
        self.cafeId = cafeId
    }
}
