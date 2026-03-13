//
//  NoticeEventUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

struct NoticeEventUiState {
    var selectedTab: NoticeEventTab = .notice
    var query: String = ""
    var notices: [NoticeItem] = []
    var events: [EventItem] = []
    var isLoadingNotices: Bool = false
    var isLoadingMoreNotices: Bool = false
    var noticeNextCursor: String? = nil
    var canLoadMoreNotices: Bool = false
    var isLoadingEvents: Bool = false
    var isLoadingMoreEvents: Bool = false
    var eventNextCursor: String? = nil
    var canLoadMoreEvents: Bool = false
    var infoMessage: String? = nil
    var isFormSheetVisible: Bool = false
    var isSubmittingForm: Bool = false
    var formEditingId: String? = nil
    var formTitle: String = ""
    var formContent: String = ""
    var formImageUrl: String = ""
    var formPinned: Bool = false
    var formReservedAt: String = ""

    var formSheetTitle: String {
        if selectedTab == .notice {
            return formEditingId == nil ? "공지사항 등록" : "공지사항 수정"
        }
        return formEditingId == nil ? "이벤트 등록" : "이벤트 수정"
    }

    var formTitlePlaceholder: String {
        selectedTab == .notice ? "제목을 입력해 주세요" : "이벤트 제목을 입력해 주세요"
    }

    var formContentPlaceholder: String {
        selectedTab == .notice ? "공지사항 내용을 입력해 주세요" : "이벤트 상세 내용을 입력해 주세요"
    }

    var formSubmitLabel: String {
        if selectedTab == .notice {
            return formEditingId == nil ? "등록하기" : "수정하기"
        }
        return formEditingId == nil ? "이벤트 등록하기" : "이벤트 수정하기"
    }

    var formScheduleLabel: String {
        selectedTab == .notice ? "게시글 예약" : "이벤트 기간"
    }

    var formSchedulePlaceholder: String {
        selectedTab == .notice ? "게시 날짜 및 시간 선택" : "이벤트 기간 선택"
    }

    var showsPinnedSection: Bool {
        selectedTab == .notice
    }

    var showsImageSection: Bool {
        selectedTab == .event
    }

    var hasAttachedImage: Bool {
        !formImageUrl.isEmpty
    }

    var formImageTitle: String {
        hasAttachedImage ? "대표 이미지 1장 첨부됨" : "대표 이미지 첨부"
    }

    var formImageDescription: String {
        "이벤트 카드에 노출되는 대표 이미지입니다. 한 장만 첨부할 수 있습니다."
    }

    var isCurrentTabLoading: Bool {
        selectedTab == .notice ? isLoadingNotices : isLoadingEvents
    }

    var isCurrentTabLoadingMore: Bool {
        selectedTab == .notice ? isLoadingMoreNotices : isLoadingMoreEvents
    }

    var isCurrentTabEmpty: Bool {
        selectedTab == .notice ? notices.isEmpty : events.isEmpty
    }

    var isFormSubmitEnabled: Bool {
        !isSubmittingForm &&
        !formTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !formContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        (!showsImageSection || hasAttachedImage)
    }
}

enum NoticeEventTab: String, CaseIterable {
    case notice = "공지사항"
    case event = "이벤트"
}

struct NoticeItem: Identifiable, Equatable {
    let id: String
    let title: String
    let content: String
    let date: String
    let isPinned: Bool
    let statusLabel: String
    let statusAccent: NoticeStatusAccent
}

struct EventItem: Identifiable, Equatable {
    let id: String
    let title: String
    let content: String
    let period: String
    let statusLabel: String
    let imageUrl: String
    let isDimmed: Bool
}

enum NoticeStatusAccent {
    case published
    case draft
    case ended
}

let sampleEventImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA"
