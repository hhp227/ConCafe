package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.Notice

interface NoticeDataSource {
    val notices: MutableList<Notice>
    val cafeNoticeManagementItems: MutableList<CafeNoticeManagementItem>
    val cafeEventManagementItems: MutableList<CafeEventManagementItem>
}