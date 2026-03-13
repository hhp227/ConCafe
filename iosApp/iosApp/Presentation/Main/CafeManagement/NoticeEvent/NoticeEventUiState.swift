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
    var notices: [NoticeItem] = NoticeItem.samples
    var events: [EventItem] = EventItem.samples
    var infoMessage: String? = nil
    var isFormSheetVisible: Bool = false
    var formTitle: String = ""
    var formContent: String = ""
    var formImageUrl: String = ""
    var formPinned: Bool = false
    var formReservedAt: String = ""

    var filteredNotices: [NoticeItem] {
        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return notices
        }
        return notices.filter { $0.title.localizedCaseInsensitiveContains(query) }
    }

    var filteredEvents: [EventItem] {
        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return events
        }
        return events.filter { $0.title.localizedCaseInsensitiveContains(query) }
    }

    var formSheetTitle: String {
        selectedTab == .notice ? "공지사항 등록" : "이벤트 등록"
    }

    var formTitlePlaceholder: String {
        selectedTab == .notice ? "제목을 입력해 주세요" : "이벤트 제목을 입력해 주세요"
    }

    var formContentPlaceholder: String {
        selectedTab == .notice ? "공지사항 내용을 입력해 주세요" : "이벤트 상세 내용을 입력해 주세요"
    }

    var formSubmitLabel: String {
        selectedTab == .notice ? "등록하기" : "이벤트 등록하기"
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

    var isFormSubmitEnabled: Bool {
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
    let date: String
    let isPinned: Bool
    let statusLabel: String
    let statusAccent: NoticeStatusAccent

    static let samples: [NoticeItem] = [
        NoticeItem(
            id: "notice-1",
            title: "[필독] 추석 연휴 영업 안내",
            date: "2024.09.10",
            isPinned: true,
            statusLabel: "게시 중",
            statusAccent: .published
        ),
        NoticeItem(
            id: "notice-2",
            title: "가을 시즌 신메뉴 라인업 공개",
            date: "2024.09.05",
            isPinned: false,
            statusLabel: "임시 저장",
            statusAccent: .draft
        ),
        NoticeItem(
            id: "notice-3",
            title: "주말 좌석 운영 정책 변경 안내",
            date: "2024.08.29",
            isPinned: false,
            statusLabel: "게시 중",
            statusAccent: .published
        )
    ]
}

struct EventItem: Identifiable, Equatable {
    let id: String
    let title: String
    let period: String
    let statusLabel: String
    let imageUrl: String
    let isDimmed: Bool

    static let samples: [EventItem] = [
        EventItem(
            id: "event-1",
            title: "여름 한정 신메뉴 출시 이벤트",
            period: "2024.06.01 - 2024.08.31",
            statusLabel: "진행 중",
            imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA",
            isDimmed: false
        ),
        EventItem(
            id: "event-2",
            title: "가정의 달 원두 1+1 기획전",
            period: "2024.05.01 - 2024.05.31",
            statusLabel: "종료",
            imageUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuDArCFaz3BWKTYq7t8oOL1HsuyHfrKScipbsR3WQ-W_afd8Yw_pYUfesi9f0iQJcZNrA5ikV_MRjFqc9S_KvTEiJnblQVm4gFSdsxXTKdjO3ZTZSw-0PkABSNHwTvHeQ1TM48PsVSw9AzZfrmmt9wrA_hNyhSL9GI859V7XvGYkXFv90pS4sAyYlc5uvrC9zSB-lVBYsyQjSwQmuQ9h9_txvxSFAcAmvXZr3DIzZ8EbYjvV04z7XQNuPJi5FiwqXya9zWY_6Zd1vw",
            isDimmed: true
        )
    ]
}

enum NoticeStatusAccent {
    case published
    case draft
    case ended
}

let sampleEventImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA"
