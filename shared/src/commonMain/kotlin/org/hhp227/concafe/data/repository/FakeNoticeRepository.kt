package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.domain.repository.NoticeRepository

class FakeNoticeRepository(
    private val dataSource: ConCafeDataSource
) : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        return dataSource.notices.take(limit.coerceAtLeast(1))
    }
}
