//
//  AppBannerLayout.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/08/09.
//

import Foundation
import Shared
import SwiftUI

enum AppBannerLayout: String, CaseIterable, Identifiable {
    case fullBleed
    case legacy

    var id: String { rawValue }

    var title: String {
        switch self {
        case .fullBleed:
            return String(localized: String.LocalizationValue("settings_banner_layout_full_bleed"), table: "Localizable")
        case .legacy:
            return String(localized: String.LocalizationValue("settings_banner_layout_legacy"), table: "Localizable")
        }
    }

    /// 풀블리드는 화면 폭을 가득 채우고, 레거시는 좌우 여백을 둔 카드 형태를 유지한다.
    var horizontalPadding: CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return 16
        }
    }

    var cornerRadius: CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return 20
        }
    }

    var skeletonCornerRadius: CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return 16
        }
    }

    /// 풀블리드 배너는 상단 바에 붙여야 하므로 피드 상단 여백을 없앤다.
    var feedTopPadding: CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return 16
        }
    }

    init(bannerLayout: BannerLayout) {
        switch bannerLayout {
        case .legacy:
            self = .legacy
        default:
            self = .fullBleed
        }
    }

    var sharedBannerLayout: BannerLayout {
        switch self {
        case .fullBleed:
            return .fullBleed
        case .legacy:
            return .legacy
        }
    }
}
