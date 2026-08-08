//
//  AppContentLayout.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/08/09.
//

import Foundation
import Shared
import SwiftUI

/// 홈 배너, 체크인 지도처럼 화면을 크게 차지하는 표시 영역의 배치 방식.
/// 두 화면이 같은 값을 공유하므로 여백/모서리 규칙도 여기에 함께 둔다.
enum AppContentLayout: String, CaseIterable, Identifiable {
    case fullBleed
    case legacy

    var id: String { rawValue }

    var title: String {
        switch self {
        case .fullBleed:
            return String(localized: String.LocalizationValue("settings_content_layout_full_bleed"), table: "Localizable")
        case .legacy:
            return String(localized: String.LocalizationValue("settings_content_layout_legacy"), table: "Localizable")
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

    /// 레거시에서만 legacyCornerRadius만큼 둥글게 자르고, 풀블리드는 각지게 둔다.
    func cornerRadius(legacy legacyCornerRadius: CGFloat) -> CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return legacyCornerRadius
        }
    }

    /// 풀블리드 표시 영역은 상단 바에 붙여야 하므로 화면 상단 여백을 없앤다.
    /// 레거시 여백은 화면마다 다르므로 호출부에서 넘긴다.
    func topPadding(legacy legacyTopPadding: CGFloat) -> CGFloat {
        switch self {
        case .fullBleed:
            return 0
        case .legacy:
            return legacyTopPadding
        }
    }

    init(contentLayout: ContentLayout) {
        switch contentLayout {
        case .legacy:
            self = .legacy
        default:
            self = .fullBleed
        }
    }

    var sharedContentLayout: ContentLayout {
        switch self {
        case .fullBleed:
            return .fullBleed
        case .legacy:
            return .legacy
        }
    }
}
