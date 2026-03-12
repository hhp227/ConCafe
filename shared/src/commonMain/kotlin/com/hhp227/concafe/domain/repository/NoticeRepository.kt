package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.Notice

interface NoticeRepository {
    suspend fun getRecentNotices(limit: Int): List<Notice>
}
