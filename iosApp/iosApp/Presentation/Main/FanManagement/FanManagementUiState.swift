//
//  FanManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct FanManagementUiState {
    var isLoading: Bool = true
    var errorMessage: String? = nil
    var fanManagementData: FanManagementData? = nil
    var castClaimStatus: CastClaimStatusCard? = nil
    var castClaimSheet: CastClaimSheet? = nil
    var isClaimSheetVisible: Bool = false
    var isAnnouncementSheetVisible: Bool = false
    var announcementTitle: String = ""
    var announcementBody: String = ""
    var isSendingAnnouncement: Bool = false
    var infoMessage: String? = nil

    struct CastClaimStatusCard: Hashable {
        let affiliatedCafeId: String
        let affiliatedCafeName: String
        let headline: String
        let body: String
        let accent: Accent
    }

    struct CastClaimSheet {
        let affiliatedCafeId: String
        let affiliatedCafeName: String
        let headline: String
        let body: String
        let requestableCasts: [CastClaimCandidate]
        let nextCursor: String?
        let canLoadMore: Bool
        let isLoadingMore: Bool
        let selectedCastId: String?
        let canSubmit: Bool
        let isSubmitting: Bool
    }

    enum QuickAction: CaseIterable, Hashable {
        case workSchedule
        case cafeDashboard

        var title: String {
            switch self {
            case .workSchedule:
                return "출근 관리"
            case .cafeDashboard:
                return "프로필 연결"
            }
        }

        var subtitle: String {
            switch self {
            case .workSchedule:
                return "이번 주 스케줄을 조정합니다."
            case .cafeDashboard:
                return "내 캐스트 프로필 연결 상태를 관리합니다."
            }
        }
    }

    enum Accent {
        case pending
        case linked
        case rejected
    }

    var isAnnouncementSubmitEnabled: Bool {
        !announcementTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !announcementBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !isSendingAnnouncement
    }

    static let empty = FanManagementUiState(
        isLoading: true,
        errorMessage: nil,
        fanManagementData: nil,
        castClaimStatus: nil,
        castClaimSheet: nil,
        isClaimSheetVisible: false,
        isAnnouncementSheetVisible: false,
        announcementTitle: "",
        announcementBody: "",
        isSendingAnnouncement: false,
        infoMessage: nil
    )
}
