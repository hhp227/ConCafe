package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

data class NoticeEventUiState(
    val selectedTab: NoticeEventTab = NoticeEventTab.NOTICE,
    val query: String = "",
    val notices: List<NoticeItem> = sampleNoticeItems(),
    val events: List<EventItem> = sampleEventItems(),
    val infoMessage: String? = null,
    val isFormSheetVisible: Boolean = false,
    val formTitle: String = "",
    val formContent: String = "",
    val formImageUrl: String = "",
    val formPinned: Boolean = false,
    val formReservedAt: String = ""
) {
    val filteredNotices: List<NoticeItem>
        get() = if (query.isBlank()) {
            notices
        } else {
            notices.filter { it.title.contains(query, ignoreCase = true) }
        }

    val filteredEvents: List<EventItem>
        get() = if (query.isBlank()) {
            events
        } else {
            events.filter { it.title.contains(query, ignoreCase = true) }
        }

    val formSheetTitle: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "공지사항 등록" else "이벤트 등록"

    val formTitlePlaceholder: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "제목을 입력해 주세요" else "이벤트 제목을 입력해 주세요"

    val formContentPlaceholder: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "공지사항 내용을 입력해 주세요" else "이벤트 상세 내용을 입력해 주세요"

    val formSubmitLabel: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "등록하기" else "이벤트 등록하기"

    val formScheduleLabel: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "게시글 예약" else "이벤트 기간"

    val formSchedulePlaceholder: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "게시 날짜 및 시간 선택" else "이벤트 기간 선택"

    val showsPinnedSection: Boolean
        get() = selectedTab == NoticeEventTab.NOTICE

    val showsImageSection: Boolean
        get() = selectedTab == NoticeEventTab.EVENT

    val hasAttachedImage: Boolean
        get() = formImageUrl.isNotBlank()

    val formImageTitle: String
        get() = if (hasAttachedImage) "대표 이미지 1장 첨부됨" else "대표 이미지 첨부"

    val formImageDescription: String
        get() = "이벤트 카드에 노출되는 대표 이미지입니다. 한 장만 첨부할 수 있습니다."

    val isFormSubmitEnabled: Boolean
        get() = formTitle.isNotBlank() && formContent.isNotBlank() && (!showsImageSection || hasAttachedImage)
}

enum class NoticeEventTab(val title: String) {
    NOTICE("공지사항"),
    EVENT("이벤트")
}

data class NoticeItem(
    val id: String,
    val title: String,
    val date: String,
    val isPinned: Boolean,
    val statusLabel: String,
    val statusAccent: NoticeStatusAccent
)

data class EventItem(
    val id: String,
    val title: String,
    val period: String,
    val statusLabel: String,
    val imageUrl: String,
    val isDimmed: Boolean = false
)

enum class NoticeStatusAccent {
    PUBLISHED,
    DRAFT,
    ENDED
}

private fun sampleNoticeItems(): List<NoticeItem> = listOf(
    NoticeItem(
        id = "notice-1",
        title = "[필독] 추석 연휴 영업 안내",
        date = "2024.09.10",
        isPinned = true,
        statusLabel = "게시 중",
        statusAccent = NoticeStatusAccent.PUBLISHED
    ),
    NoticeItem(
        id = "notice-2",
        title = "가을 시즌 신메뉴 라인업 공개",
        date = "2024.09.05",
        isPinned = false,
        statusLabel = "임시 저장",
        statusAccent = NoticeStatusAccent.DRAFT
    ),
    NoticeItem(
        id = "notice-3",
        title = "주말 좌석 운영 정책 변경 안내",
        date = "2024.08.29",
        isPinned = false,
        statusLabel = "게시 중",
        statusAccent = NoticeStatusAccent.PUBLISHED
    )
)

private fun sampleEventItems(): List<EventItem> = listOf(
    EventItem(
        id = "event-1",
        title = "여름 한정 신메뉴 출시 이벤트",
        period = "2024.06.01 - 2024.08.31",
        statusLabel = "진행 중",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA"
    ),
    EventItem(
        id = "event-2",
        title = "가정의 달 원두 1+1 기획전",
        period = "2024.05.01 - 2024.05.31",
        statusLabel = "종료",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDArCFaz3BWKTYq7t8oOL1HsuyHfrKScipbsR3WQ-W_afd8Yw_pYUfesi9f0iQJcZNrA5ikV_MRjFqc9S_KvTEiJnblQVm4gFSdsxXTKdjO3ZTZSw-0PkABSNHwTvHeQ1TM48PsVSw9AzZfrmmt9wrA_hNyhSL9GI859V7XvGYkXFv90pS4sAyYlc5uvrC9zSB-lVBYsyQjSwQmuQ9h9_txvxSFAcAmvXZr3DIzZ8EbYjvV04z7XQNuPJi5FiwqXya9zWY_6Zd1vw",
        isDimmed = true
    )
)

internal const val SAMPLE_EVENT_IMAGE_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA"
