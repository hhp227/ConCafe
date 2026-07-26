package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.model.InquiryStatus
import com.hhp227.concafe.domain.repository.InquiryRepository

class FakeInquiryRepository(
    private val dataSource: ConCafeDataSource
) : InquiryRepository {
    override suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry {
        if (input.inquiryType.isBlank()) throw IllegalArgumentException("문의 유형을 선택해 주세요.")
        if (input.title.isBlank()) throw IllegalArgumentException("문의 제목을 입력해 주세요.")
        if (input.content.isBlank()) throw IllegalArgumentException("문의 내용을 입력해 주세요.")

        val nextIndex = dataSource.inquiries.size + 1
        val inquiry = Inquiry(
            id = "inquiry-$nextIndex",
            userId = userId,
            userNickname = userNickname,
            inquiryType = input.inquiryType.trim(),
            title = input.title.trim(),
            content = input.content.trim(),
            status = InquiryStatus.PENDING,
            createdAt = "2026-03-16T10:${(nextIndex % 60).toString().padStart(2, '0')}:00Z",
            createdAtLabel = "방금 전"
        )
        dataSource.inquiries.add(0, inquiry)
        return inquiry
    }

    override suspend fun getInquiryPage(cursor: String?, pageSize: Int): PagedResult<Inquiry> {
        val safePageSize = pageSize.coerceAtLeast(1)
        val sorted = dataSource.inquiries.sortedByDescending { inquiry ->
            inquiry.createdAt
        }
        val startIndex = cursor?.toIntOrNull() ?: 0
        val endIndex = (startIndex + safePageSize).coerceAtMost(sorted.size)
        val hasNext = endIndex < sorted.size
        val nextCursor = if (hasNext) endIndex.toString() else null
        return PagedResult(
            items = sorted.subList(startIndex, endIndex),
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }
}
