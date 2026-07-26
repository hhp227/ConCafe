package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem

data class NoticeEventUiState(
    val selectedTab: NoticeEventTab = NoticeEventTab.NOTICE,
    val query: String = "",
    val notices: List<CafeNoticeManagementItem> = emptyList(),
    val events: List<CafeEventManagementItem> = emptyList(),
    val cafeCasts: List<CafeCastPreview> = emptyList(),
    val isLoadingCafeCasts: Boolean = false,
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
    val formReservedAt: String = "",
    val formParticipantCastIds: List<String> = emptyList(),
    val formHasLivePerformance: Boolean = false
) {
    val showsPinnedSection: Boolean
        get() = selectedTab == NoticeEventTab.NOTICE

    val showsImageSection: Boolean
        get() = selectedTab == NoticeEventTab.EVENT

    val hasAttachedImage: Boolean
        get() = formImageUrl.isNotBlank()

    val isCurrentTabLoading: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) isLoadingNotices else isLoadingEvents

    val isCurrentTabLoadingMore: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) isLoadingMoreNotices else isLoadingMoreEvents

    val isCurrentTabEmpty: Boolean
        get() = if (selectedTab == NoticeEventTab.NOTICE) notices.isEmpty() else events.isEmpty()

    val isFormSubmitEnabled: Boolean
        get() = !isSubmittingForm && formTitle.isNotBlank() && formContent.isNotBlank() && (!showsImageSection || hasAttachedImage)

    val selectedParticipantCasts: List<CafeCastPreview>
        get() = cafeCasts.filter { it.id in formParticipantCastIds }
}

enum class NoticeEventTab {
    NOTICE,
    EVENT
}

