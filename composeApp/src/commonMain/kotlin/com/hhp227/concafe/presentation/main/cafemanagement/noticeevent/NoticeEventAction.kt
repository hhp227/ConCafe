package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

sealed interface NoticeEventAction {
    data object ClickBack : NoticeEventAction
    data class SelectTab(val tab: NoticeEventTab) : NoticeEventAction
    data class ChangeQuery(val value: String) : NoticeEventAction
    data object LoadMoreNotices : NoticeEventAction
    data object LoadMoreEvents : NoticeEventAction
    data object ClickRegister : NoticeEventAction
    data object ClickMoreEvents : NoticeEventAction
    data class ClickEditNotice(val id: String) : NoticeEventAction
    data class ClickDeleteNotice(val id: String) : NoticeEventAction
    data class ClickEditEvent(val id: String) : NoticeEventAction
    data class ClickDeleteEvent(val id: String) : NoticeEventAction
    data object DismissFormSheet : NoticeEventAction
    data class ChangeFormTitle(val value: String) : NoticeEventAction
    data class ChangeFormContent(val value: String) : NoticeEventAction
    data object ClickFormImage : NoticeEventAction
    data object ClickRemoveFormImage : NoticeEventAction
    data class ChangeFormPinned(val value: Boolean) : NoticeEventAction
    data object ClickReserveSchedule : NoticeEventAction
    data object ClickSubmitForm : NoticeEventAction
    data object DismissInfoMessage : NoticeEventAction
}
