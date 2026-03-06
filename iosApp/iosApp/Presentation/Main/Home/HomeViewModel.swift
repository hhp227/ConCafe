//
//  HomeViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine

@MainActor
final class HomeViewModel: ObservableObject {
    private let getHomeFeedUseCase: GetHomeFeedUseCase
    
    @Published private(set) var uiState = HomeUiState.empty

    let event = PassthroughSubject<HomeEvent, Never>()

    private func loadHomeFeed() {
        let result = getHomeFeedUseCase.invoke(limit: 10)
        let feed = MockConCafeDataSource().homeFeed
        uiState = mapHomeFeed(feed)
    }

    private func mapHomeFeed(_ feed: Shared.HomeFeed) -> HomeUiState {
        let banners = (feed.banners as? [Shared.HomeBanner] ?? []).map { banner in
            HomeBanner(
                id: banner.id,
                title: banner.title,
                colors: [Color(hex: banner.startColorHex), Color(hex: banner.endColorHex)]
            )
        }

        let popularMaids = (feed.popularCasts as? [Shared.HomePopularCast] ?? []).map { cast in
            PopularMaid(
                id: cast.id,
                name: cast.name,
                cafe: cast.cafeName,
                followers: Int(cast.followers)
            )
        }

        let nearbyCafes = (feed.nearbyCafes as? [Shared.HomeNearbyCafe] ?? []).map { cafe in
            NearbyCafe(
                id: cafe.id,
                name: cafe.name,
                rating: "\(cafe.rating)",
                location: cafe.location,
                distance: cafe.distance
            )
        }

        let birthdayMaids = (feed.birthdayCasts as? [Shared.HomeBirthdayCast] ?? []).map { cast in
            BirthdayMaid(
                id: cast.id,
                name: cast.name
            )
        }

        let notices = (feed.notices as? [Shared.Notice] ?? []).map { notice in
            NoticeItem(
                id: notice.id,
                cafe: notice.cafeName,
                content: notice.content,
                time: notice.relativeTime
            )
        }

        return HomeUiState(
            banners: banners,
            popularMaids: popularMaids,
            nearbyCafes: nearbyCafes,
            birthdayMaids: birthdayMaids,
            notices: notices
        )
    }

    func onAction(_ action: HomeAction) {
        switch action {
        case .maidTapped(let id):
            event.send(.navigateToCastDetail(id: id))
        case .birthdayMaidTapped(let id):
            event.send(.navigateToCastDetail(id: id))
        case .cafeTapped(let id):
            event.send(.navigateToCafeDetail(id: id))
        }
    }

    init() {
        loadHomeFeed()
    }
}
