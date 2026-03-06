package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.Notice

interface NoticeRepository {
    suspend fun getRecentNotices(limit: Int): List<Notice>
}
