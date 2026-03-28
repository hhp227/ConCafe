package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.InquiryDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.model.InquiryStatus
import com.hhp227.concafe.domain.repository.InquiryRepository
import kotlinx.datetime.Clock

class InquiryRepositoryImpl(
    private val inquiryDataSource: InquiryDataSource,
    private val pagingDataSource: PagingDataSource
) : InquiryRepository {
    override suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry {
        if (input.inquiryType.isBlank()) throw IllegalArgumentException("문의 유형을 선택해 주세요.")
        if (input.title.isBlank()) throw IllegalArgumentException("문의 제목을 입력해 주세요.")
        if (input.content.isBlank()) throw IllegalArgumentException("문의 내용을 입력해 주세요.")
        (inquiryDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.createInquiryRemote(
                userId = userId,
                userNickname = userNickname,
                input = input
            )
        }

        val inquiry = Inquiry(
            id = nextEntityId("inquiry"),
            userId = userId,
            userNickname = userNickname,
            inquiryType = input.inquiryType.trim(),
            title = input.title.trim(),
            content = input.content.trim(),
            status = InquiryStatus.PENDING,
            createdAt = nowIsoUtc(),
            createdAtLabel = "방금 전"
        )
        inquiryDataSource.inquiries.add(0, inquiry)
        return inquiry
    }

    override suspend fun getInquiryPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Inquiry> {
        val safePageSize = pageSize.coerceAtLeast(1)
        (inquiryDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.getInquiryPageRemote(
                cursor = cursor,
                pageSize = safePageSize
            )
        }
        val sorted = inquiryDataSource.inquiries.sortedByDescending { inquiry ->
            inquiry.createdAt
        }
        return pagingDataSource.toPaged(sorted, cursor, safePageSize)
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
