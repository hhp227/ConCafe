package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem

sealed class NoticeManagementEvent {
    data class NoticeCreated(val cafeId: String) : NoticeManagementEvent()
    data class NoticeUpdated(val cafeId: String, val notice: CafeNoticeManagementItem) : NoticeManagementEvent()
    data class NoticeDeleted(val cafeId: String, val noticeId: String) : NoticeManagementEvent()
    data class EventCreated(val cafeId: String) : NoticeManagementEvent()
    data class EventUpdated(val cafeId: String, val event: CafeEventManagementItem) : NoticeManagementEvent()
    data class EventDeleted(val cafeId: String, val eventId: String) : NoticeManagementEvent()
}