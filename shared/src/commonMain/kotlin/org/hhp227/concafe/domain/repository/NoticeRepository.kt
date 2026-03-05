package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.Notice

interface NoticeRepository {
    suspend fun getCafeNotices(cafeId: String, limit: Int): List<Notice>
}
