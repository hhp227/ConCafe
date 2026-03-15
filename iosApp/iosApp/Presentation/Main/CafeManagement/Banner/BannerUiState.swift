//
//  BannerUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

struct BannerUiState {
    var screenTitle = "배너 관리"
    var selectedTab: BannerTab = .active
    var locationLabel = "홈 화면 상단"
    var banners: [BannerItem] = sampleBannerItems()

    var filteredBanners: [BannerItem] {
        banners.filter { $0.tab == selectedTab }
    }

    var sectionTitle: String {
        switch selectedTab {
        case .active:
            return "현재 노출 중인 배너"
        case .scheduled:
            return "노출 예정 배너"
        case .ended:
            return "종료된 배너"
        }
    }

    var sectionCountLabel: String {
        "\(sectionTitle) (\(filteredBanners.count))"
    }
}

struct BannerItem: Identifiable {
    let id: String
    let title: String
    let description: String
    let periodText: String
    let statusLabel: String
    let tab: BannerTab
    let accentHex: String
    let imageIcon: String
}

enum BannerTab: String, CaseIterable {
    case active = "진행 중"
    case scheduled = "예약"
    case ended = "종료"
}

private func sampleBannerItems() -> [BannerItem] {
    [
        BannerItem(
            id: "banner-active-1",
            title: "겨울 시즌 딸기 라떼 할인",
            description: "상큼한 딸기 메뉴 20% 할인 이벤트",
            periodText: "2023.12.01 ~ 2023.12.31",
            statusLabel: "진행 중",
            tab: .active,
            accentHex: "F27CA6",
            imageIcon: "cup.and.saucer.fill"
        ),
        BannerItem(
            id: "banner-active-2",
            title: "신규 멤버십 가입 혜택",
            description: "지금 가입하면 아메리카노 1잔 무료",
            periodText: "2023.11.15 ~ 2024.01.15",
            statusLabel: "진행 중",
            tab: .active,
            accentHex: "CE6A8C",
            imageIcon: "gift.fill"
        ),
        BannerItem(
            id: "banner-active-3",
            title: "주말 특별 연주회 안내",
            description: "매주 토요일 오후 3시, 라이브 재즈",
            periodText: "2023.12.01 ~ 2023.12.31",
            statusLabel: "진행 중",
            tab: .active,
            accentHex: "8A5B73",
            imageIcon: "music.note"
        ),
        BannerItem(
            id: "banner-scheduled-1",
            title: "화이트데이 디저트 페어",
            description: "화이트 초콜릿 디저트 라인업 미리보기",
            periodText: "2024.03.10 ~ 2024.03.17",
            statusLabel: "예약",
            tab: .scheduled,
            accentHex: "F3AFC5",
            imageIcon: "birthday.cake.fill"
        ),
        BannerItem(
            id: "banner-scheduled-2",
            title: "신메뉴 브런치 런칭",
            description: "주말 브런치 신메뉴를 곧 공개합니다",
            periodText: "2024.03.20 ~ 2024.04.20",
            statusLabel: "예약",
            tab: .scheduled,
            accentHex: "D78EA9",
            imageIcon: "fork.knife"
        ),
        BannerItem(
            id: "banner-ended-1",
            title: "연말 감사 쿠폰 이벤트",
            description: "방문 고객 대상 감사 쿠폰 증정",
            periodText: "2023.12.20 ~ 2024.01.05",
            statusLabel: "종료",
            tab: .ended,
            accentHex: "BDA3AE",
            imageIcon: "ticket.fill"
        ),
        BannerItem(
            id: "banner-ended-2",
            title: "설 연휴 운영 안내",
            description: "설 연휴 영업시간과 휴무일 안내",
            periodText: "2024.02.07 ~ 2024.02.12",
            statusLabel: "종료",
            tab: .ended,
            accentHex: "94808A",
            imageIcon: "calendar"
        )
    ]
}
