package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.repository.NoticeRepository

class FakeNoticeRepository(
    private val dataSource: ConCafeDataSource
) : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        return dataSource.notices.take(limit.coerceAtLeast(1))
    }
}
