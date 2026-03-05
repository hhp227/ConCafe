package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.domain.repository.NoticeRepository

class FakeNoticeRepository(
    private val dataSource: ConCafeDataSource
) : NoticeRepository {
    override suspend fun getCafeNotices(cafeId: String, limit: Int): List<Notice> {
        return dataSource.notices.filter { it.cafeId == cafeId }.take(limit.coerceAtLeast(1))
    }
}
