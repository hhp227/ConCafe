package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

data class NoticeEventUiState(
    val selectedTab: NoticeEventTab = NoticeEventTab.NOTICE,
    val query: String = "",
    val notices: List<NoticeItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val isLoadingNotices: Boolean = false,
    val isLoadingMoreNotices: Boolean = false,
    val noticeNextCursor: String? = null,
    val canLoadMoreNotices: Boolean = false,
    val isLoadingEvents: Boolean = false,
    val isLoadingMoreEvents: Boolean = false,
    val eventNextCursor: String? = null,
    val canLoadMoreEvents: Boolean = false,
    val infoMessage: String? = null,
    val isFormSheetVisible: Boolean = false,
    val isSubmittingForm: Boolean = false,
    val formEditingId: String? = null,
    val formTitle: String = "",
    val formContent: String = "",
    val formImageUrl: String = "",
    val formPinned: Boolean = false,
    val formReservedAt: String = ""
) {
    val formSheetTitle: String
        get() = when {
            selectedTab == NoticeEventTab.NOTICE && formEditingId != null -> "공지사항 수정"
            selectedTab == NoticeEventTab.NOTICE -> "공지사항 등록"
            formEditingId != null -> "이벤트 수정"
            else -> "이벤트 등록"
        }

    val formTitlePlaceholder: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "제목을 입력해 주세요" else "이벤트 제목을 입력해 주세요"

    val formContentPlaceholder: String
        get() = if (selectedTab == NoticeEventTab.NOTICE) "공지사항 내용을 입력해 주세요" else "이벤트 상세 내용을 입력해 주세요"

    val formSubmitLabel: String
        get() = when {
            selectedTab == NoticeEventTab.NOTICE && formEditingId != null -> "수정하기"
            selectedTab == NoticeEventTab.NOTICE -> "등록하기"
            formEditingId != null -> "이벤트 수정하기"
            else -> "이벤트 등록하기"
        }

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

    val isCurrentTabLoading: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) isLoadingNotices else isLoadingEvents

    val isCurrentTabLoadingMore: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) isLoadingMoreNotices else isLoadingMoreEvents

    val isCurrentTabEmpty: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) notices.isEmpty() else events.isEmpty()

    val isFormSubmitEnabled: Boolean
        get() = !isSubmittingForm && formTitle.isNotBlank() && formContent.isNotBlank() && (!showsImageSection || hasAttachedImage)
}

enum class NoticeEventTab(val title: String) {
    NOTICE("공지사항"),
    EVENT("이벤트")
}

data class NoticeItem(
    val id: String,
    val title: String,
    val content: String,
    val date: String,
    val isPinned: Boolean,
    val statusLabel: String,
    val statusAccent: NoticeStatusAccent
)

data class EventItem(
    val id: String,
    val title: String,
    val content: String,
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

